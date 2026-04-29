package com.streamx.cli.commands.settings.eventtemplates.which;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import com.streamx.cli.commands.publish.event.EventTemplateCatalog.TemplateLocation;
import com.streamx.cli.commands.settings.eventtemplates.TemplateIdCompletionCandidates;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.InteractivePicker;
import picocli.CommandLine;

@CommandLine.Command(
    name = "which",
    header = "Print the resolved file path for a template ID",
    description = "Prints only the absolute path that `publish event <templateId>` would "
        + "resolve to. Useful for scripting: `$(streamx settings event-templates which "
        + "page.published)`.",
    footer = {
        "",
        "Examples:",
        "  streamx settings event-templates which page.published",
        "  cat \"$(streamx settings event-templates which page.published)\"",
        "  streamx settings event-templates which -o json page.published"
    }
)
public class WhichCommand extends AbstractCommand<TemplateLocation> {

  @CommandLine.Parameters(
      index = "0",
      arity = "0..1",
      description = "Template ID (prompts if omitted)",
      completionCandidates = TemplateIdCompletionCandidates.class
  )
  public String templateId;

  @Override
  public CommandResult<TemplateLocation> runCommand() {
    if (EventTemplateCatalog.listAll().isEmpty()) {
      throw new CliException(msg.eventTemplatesNoTemplatesFound());
    }

    String input = templateId;
    if (input == null || input.isBlank()) {
      input = InteractivePicker.pick(
          msg.eventTemplateWhichPrompt(), EventTemplateCatalog.templateIds());
      if (input == null || input.isBlank()) {
        throw new CliException(msg.eventTemplateIdRequired());
      }
    }
    final String resolved = input;
    TemplateLocation found = EventTemplateCatalog.findById(resolved)
        .orElseThrow(() -> new CliException(msg.eventTemplateNotFound(resolved)));
    return new CommandResult<>(found);
  }

  @Override
  public String getTextOutput(CommandResult<TemplateLocation> result) {

    return result.getData().path();
  }
}
