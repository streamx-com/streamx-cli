package com.streamx.cli.commands.settings.eventtemplates.delete;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import com.streamx.cli.commands.publish.event.EventTemplateCatalog.TemplateLocation;
import com.streamx.cli.commands.settings.eventtemplates.NonDefaultTemplateIdCompletionCandidates;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.InteractivePicker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import picocli.CommandLine;

@CommandLine.Command(
    name = "delete",
    header = "Delete a user-created event template",
    description = {
        "Deletes a template from the context's event-templates/ folder.",
        "Default templates cannot be deleted this way - use "
            + "`reset-default-templates` to restore them.",
        "Registered templates cannot be deleted this way - use `unregister` instead."
    },
    footer = {
        "",
        "Examples:",
        "  streamx settings event-templates delete my.custom",
        "  streamx settings event-templates delete my.custom --force",
        "  streamx settings event-templates delete   # picks interactively"
    }
)
public class DeleteCommand extends AbstractCommand<DeleteCommandResult> {

  @CommandLine.Parameters(
      index = "0",
      arity = "0..1",
      description = "Template ID (prompts if omitted)",
      completionCandidates = NonDefaultTemplateIdCompletionCandidates.class
  )
  public String templateId;

  @CommandLine.Option(
      names = {"-f", "--force"},
      description = "Skip the confirmation prompt"
  )
  public boolean force;

  @Override
  public CommandResult<DeleteCommandResult> runCommand() {
    TemplateLocation found = resolveTemplate();

    switch (found.source()) {
      case EventTemplateCatalog.SOURCE_DEFAULT:
        throw new CliException(msg.eventTemplateCannotDeleteDefault());
      case EventTemplateCatalog.SOURCE_SETTINGS:
        throw new CliException(msg.eventTemplateCannotDeleteRegistered(found.id()));
      default:
        break;
    }

    if (!force) {
      String answer = InteractivePicker.pick(
          msg.eventTemplateDeleteConfirm(found.id(), found.path()), null);
      if (answer == null || !isYes(answer)) {
        throw new CliException(msg.eventTemplateDeleteCancelled());
      }
    }

    Path file = Path.of(found.path());
    try {
      Files.deleteIfExists(file);
    } catch (IOException e) {
      throw new CliException(msg.pathDeleteFailed(found.path(), e.getMessage()), e);
    }

    return new CommandResult<>(new DeleteCommandResult(found.id(), found.path()));
  }

  @Override
  public String getTextOutput(CommandResult<DeleteCommandResult> result) {
    return msg.eventTemplateDeleted(result.getData().id());
  }

  private TemplateLocation resolveTemplate() {
    if (EventTemplateCatalog.listAll().isEmpty()) {
      throw new CliException(msg.eventTemplatesNoTemplatesFound());
    }
    String input = templateId;
    if (input == null || input.isBlank()) {
      input = InteractivePicker.pick(
          msg.eventTemplateDeletePrompt(), EventTemplateCatalog.templateIds());
      if (input == null || input.isBlank()) {
        throw new CliException(msg.eventTemplateIdRequired());
      }
    }
    final String resolved = input;
    return EventTemplateCatalog.findById(resolved)
        .orElseThrow(() -> new CliException(msg.eventTemplateNotFound(resolved)));
  }

  private static boolean isYes(String answer) {
    String s = answer.strip().toLowerCase();
    return "y".equals(s) || "yes".equals(s);
  }
}
