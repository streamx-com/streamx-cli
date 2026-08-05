package com.streamx.cli.commands.settings.eventtemplates.copy;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import com.streamx.cli.commands.publish.event.EventTemplateCatalog.TemplateLocation;
import com.streamx.cli.commands.publish.event.UserEventTemplates;
import com.streamx.cli.commands.settings.eventtemplates.TemplateIdCompletionCandidates;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.InteractivePicker;
import com.streamx.cli.framework.InteractivePicker.Session;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import picocli.CommandLine;

@CommandLine.Command(
    name = "copy",
    header = "Copy an existing event template under a new ID",
    description = "Copies the resolved content of <sourceId> into the context's "
        + "event-templates/<destId>.json. Works with templates from any "
        + "source (default / custom / registered in settings). "
        + "The copy always lands in the context's event-templates folder.",
    footer = {
        "",
        "Examples:",
        "  streamx settings event-templates copy page.published my.page",
        "  streamx settings event-templates copy   # interactive"
    }
)
public class CopyCommand extends AbstractCommand<CopyCommandResult> {

  @CommandLine.Parameters(
      index = "0",
      arity = "0..1",
      description = "Source template ID to copy from (prompts if omitted)",
      completionCandidates = TemplateIdCompletionCandidates.class
  )
  public String sourceId;

  @CommandLine.Parameters(
      index = "1",
      arity = "0..1",
      description = "Destination template ID (prompts if omitted)"
  )
  public String destId;

  @Override
  public CommandResult<CopyCommandResult> runCommand() {
    if (EventTemplateCatalog.listAll().isEmpty()) {
      throw new CliException(msg.eventTemplatesNoTemplatesFound());
    }

    TemplateLocation source;
    String dest;

    try (Session session = InteractivePicker.open()) {
      source = resolveSource(session);
      dest = resolveDestId(session);
    }

    Path destPath = UserEventTemplates.resolve(dest);
    if (EventTemplateCatalog.findById(dest).isPresent() || Files.exists(destPath)) {
      throw new CliException(msg.eventTemplateAlreadyExists(
          dest, destPath.toAbsolutePath().toString()));
    }

    try {
      Files.createDirectories(destPath.getParent());
      Files.copy(Path.of(source.path()), destPath);
    } catch (IOException e) {
      throw new CliException(
          msg.failedToCopyEventTemplateTo(
              source.id(), destPath.toAbsolutePath().toString(), e.getMessage()),
          e);
    }

    return new CommandResult<>(new CopyCommandResult(
        source.id(), dest, destPath.toAbsolutePath().toString()));
  }

  @Override
  public String getTextOutput(CommandResult<CopyCommandResult> result) {
    CopyCommandResult data = result.getData();
    return msg.eventTemplateCopied(data.sourceId(), data.destId(), data.path());
  }

  private TemplateLocation resolveSource(Session session) {
    String input = sourceId;
    if (input == null || input.isBlank()) {
      input = session.pick(
          msg.eventTemplateCopySourcePrompt(), EventTemplateCatalog.templateIds());
      if (input == null || input.isBlank()) {
        throw new CliException(msg.eventTemplateIdRequired());
      }
    }
    final String resolved = input;
    return EventTemplateCatalog.findById(resolved)
        .orElseThrow(() -> new CliException(msg.eventTemplateNotFound(resolved)));
  }

  private String resolveDestId(Session session) {
    String resolved = destId;
    if (resolved == null || resolved.isBlank()) {
      resolved = session.pick(msg.eventTemplateCopyDestPrompt(), null);
      if (resolved == null || resolved.isBlank()) {
        throw new CliException(msg.eventTemplateIdRequired());
      }
    }
    return resolved;
  }
}
