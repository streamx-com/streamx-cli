package com.streamx.cli.commands.settings.eventtemplates.rename;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import com.streamx.cli.commands.publish.event.EventTemplateCatalog.TemplateLocation;
import com.streamx.cli.commands.publish.event.EventTemplateLoader;
import com.streamx.cli.commands.publish.event.UserEventTemplates;
import com.streamx.cli.commands.settings.eventtemplates.NonDefaultTemplateIdCompletionCandidates;
import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.InteractivePicker;
import com.streamx.cli.framework.InteractivePicker.Session;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import picocli.CommandLine;

@CommandLine.Command(
    name = "rename",
    header = "Rename an event template",
    description = {
        "For user-created templates: renames the file in the context's event-templates/ folder.",
        "For registered templates: rewrites the settings entry under the new ID "
            + "(the underlying file is not moved).",
        "Default templates cannot be renamed - use `copy` to create a clone under a new ID."
    },
    footer = {
        "",
        "Examples:",
        "  streamx settings event-templates rename my.old my.new",
        "  streamx settings event-templates rename   # interactive"
    }
)
public class RenameCommand extends AbstractCommand<RenameCommandResult> {

  @CommandLine.Parameters(
      index = "0",
      arity = "0..1",
      description = "Current template ID (prompts if omitted)",
      completionCandidates = NonDefaultTemplateIdCompletionCandidates.class
  )
  public String oldId;

  @CommandLine.Parameters(
      index = "1",
      arity = "0..1",
      description = "New template ID (prompts if omitted)"
  )
  public String newId;

  @Override
  public CommandResult<RenameCommandResult> runCommand() {
    if (EventTemplateCatalog.listAll().isEmpty()) {
      throw new CliException(msg.eventTemplatesNoTemplatesFound());
    }

    TemplateLocation source;
    String dest;
    try (Session session = InteractivePicker.open()) {
      source = resolveOld(session);
      dest = resolveNew(session);
    }

    if (dest.equals(source.id())) {

      return new CommandResult<>(new RenameCommandResult(
          source.id(), dest, source.source(), source.path()));
    }

    if (EventTemplateCatalog.findById(dest).isPresent()) {
      throw new CliException(msg.eventTemplateAlreadyExists(
          dest, EventTemplateCatalog.findById(dest).get().path()));
    }

    return switch (source.source()) {
      case EventTemplateCatalog.SOURCE_DEFAULT ->
          throw new CliException(msg.eventTemplateCannotRenameDefault());
      case EventTemplateCatalog.SOURCE_CUSTOM -> renameUserFile(source, dest);
      case EventTemplateCatalog.SOURCE_SETTINGS -> renameSettingsEntry(source, dest);
      default -> throw new CliException(msg.eventTemplateNotFound(source.id()));
    };
  }

  @Override
  public String getTextOutput(CommandResult<RenameCommandResult> result) {
    RenameCommandResult data = result.getData();
    return msg.eventTemplateRenamed(data.oldId(), data.newId());
  }

  private CommandResult<RenameCommandResult> renameUserFile(
      TemplateLocation source, String dest) {
    Path oldPath = Path.of(source.path());
    Path newPath = UserEventTemplates.resolve(dest);
    try {
      Files.move(oldPath, newPath);
    } catch (IOException e) {
      throw new CliException(msg.pathDeleteFailed(newPath.toAbsolutePath().toString(),
          e.getMessage()), e);
    }
    return new CommandResult<>(new RenameCommandResult(
        source.id(), dest, source.source(), newPath.toAbsolutePath().toString()));
  }

  private CommandResult<RenameCommandResult> renameSettingsEntry(
      TemplateLocation source, String dest) {
    URL url = StreamxHome.getConfigUrl();
    Path configPath = Paths.get(url.getPath());
    Properties properties = new Properties();
    try (InputStream inputStream = url.openStream()) {
      properties.load(inputStream);
    } catch (IOException e) {
      throw new CliException(msg.unableToGetSettingsProperty(e.getMessage()), e);
    }
    String oldKey = EventTemplateLoader.TEMPLATE_SETTINGS_MAPPING_PREFIX + source.id();
    String newKey = EventTemplateLoader.TEMPLATE_SETTINGS_MAPPING_PREFIX + dest;
    String value = properties.getProperty(oldKey);
    if (value == null) {

      throw new CliException(msg.eventTemplateNotRegisteredInSettings(source.id()));
    }
    properties.remove(oldKey);
    properties.setProperty(newKey, value);
    try (OutputStream outputStream = Files.newOutputStream(configPath)) {
      properties.store(outputStream, null);
    } catch (IOException e) {
      throw new CliException(msg.unableToSetSettingsProperty(), e);
    }
    return new CommandResult<>(new RenameCommandResult(
        source.id(), dest, source.source(), source.path()));
  }

  private TemplateLocation resolveOld(Session session) {
    String input = oldId;
    if (input == null || input.isBlank()) {
      input = session.pick(
          msg.eventTemplateRenamePrompt(), EventTemplateCatalog.templateIds());
      if (input == null || input.isBlank()) {
        throw new CliException(msg.eventTemplateIdRequired());
      }
    }
    final String resolved = input;
    return EventTemplateCatalog.findById(resolved)
        .orElseThrow(() -> new CliException(msg.eventTemplateNotFound(resolved)));
  }

  private String resolveNew(Session session) {
    String resolved = newId;
    if (resolved == null || resolved.isBlank()) {
      resolved = session.pick(msg.eventTemplateRenameDestPrompt(), null);
      if (resolved == null || resolved.isBlank()) {
        throw new CliException(msg.eventTemplateIdRequired());
      }
    }
    return resolved;
  }
}
