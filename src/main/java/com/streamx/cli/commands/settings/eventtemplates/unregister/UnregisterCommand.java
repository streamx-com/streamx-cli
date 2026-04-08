package com.streamx.cli.commands.settings.eventtemplates.unregister;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import com.streamx.cli.commands.publish.event.EventTemplateLoader;
import com.streamx.cli.commands.settings.eventtemplates.TemplateIdCompletionCandidates;
import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.InteractivePicker;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import picocli.CommandLine;

@CommandLine.Command(
    name = "unregister",
    header = "Unregister a settings-registered event template (defaults are not affected)",
    description = "Removes an `eventtemplate.<id>` entry from "
        + "<streamxHome>/config/application.properties. The underlying file on disk is not "
        + "touched. This command only operates on settings-registered templates — it will "
        + "not delete defaults or user-created templates.",
    footer = {
        "",
        "Examples:",
        "  streamx settings event-templates unregister my.alias",
        "  streamx settings event-templates unregister   # picks interactively"
    }
)
public class UnregisterCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(
      index = "0",
      arity = "0..1",
      description = "Template ID to unregister (prompts if omitted)",
      completionCandidates = TemplateIdCompletionCandidates.class
  )
  public String templateId;

  @Override
  public CommandResult<Void> runCommand() throws RuntimeException {
    Map<String, String> registrations = EventTemplateCatalog.listSettingsRegistrations();

    if (registrations.isEmpty()) {
      throw new CliException(msg.eventTemplateNoSettingsRegistrations());
    }

    String resolved = templateId;
    if (resolved == null || resolved.isBlank()) {
      List<String> options = List.copyOf(registrations.keySet());
      resolved = InteractivePicker.pick(msg.eventTemplateUnregisterPrompt(), options);
      if (resolved == null || resolved.isBlank()) {
        throw new CliException(msg.eventTemplateIdRequired());
      }
    }

    if (!registrations.containsKey(resolved)) {
      throw new CliException(msg.eventTemplateNotRegisteredInSettings(resolved));
    }

    URL url = StreamxHome.getConfigUrl();
    Path configPath = Paths.get(url.getPath());
    Properties properties = new Properties();
    try (InputStream inputStream = url.openStream()) {
      properties.load(inputStream);
    } catch (IOException e) {
      throw new CliException(msg.unableToUnsetSettingsProperty(resolved, e.getMessage()), e);
    }

    properties.remove(EventTemplateLoader.TEMPLATE_SETTINGS_MAPPING_PREFIX + resolved);

    try (OutputStream outputStream = Files.newOutputStream(configPath)) {
      properties.store(outputStream, null);
    } catch (IOException e) {
      throw new CliException(msg.unableToUnsetSettingsProperty(resolved, e.getMessage()), e);
    }

    return new CommandResult<>(null);
  }
}
