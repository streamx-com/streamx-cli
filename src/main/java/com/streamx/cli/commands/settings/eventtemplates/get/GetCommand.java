package com.streamx.cli.commands.settings.eventtemplates.get;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import com.streamx.cli.commands.publish.event.EventTemplateCatalog.TemplateLocation;
import com.streamx.cli.commands.settings.eventtemplates.TemplateIdCompletionCandidates;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.InteractivePicker;
import java.nio.file.Files;
import java.nio.file.Path;
import picocli.CommandLine;

@CommandLine.Command(
    name = "get",
    header = "Show the content of an event template",
    description = "Prints the template JSON as-is (text), or reformats it (json / yaml).",
    footer = {
        "",
        "Examples:",
        "  streamx settings event-templates get page.published",
        "  streamx settings event-templates get page.published -o yaml",
        "  streamx settings event-templates get   # picks interactively"
    }
)
public class GetCommand extends AbstractCommand<JsonNode> {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @CommandLine.Parameters(
      index = "0",
      arity = "0..1",
      description = "Template ID (prompts if omitted)",
      completionCandidates = TemplateIdCompletionCandidates.class
  )
  public String templateId;

  @Override
  public CommandResult<JsonNode> runCommand() {
    TemplateLocation found = resolveTemplate();
    Path file = Path.of(found.path());
    if (!Files.isRegularFile(file)) {
      throw new CliException(msg.eventTemplateFileMissing(found.path()));
    }
    String raw;
    try {
      raw = Files.readString(file);
    } catch (Exception e) {
      throw new CliException(msg.eventTemplateFileMissing(found.path()), e);
    }
    JsonNode parsed;
    try {
      parsed = MAPPER.readTree(raw);
    } catch (Exception e) {
      throw new CliException(msg.invalidEventTemplate(found.id()), e);
    }
    CommandResult<JsonNode> result = new CommandResult<>(parsed);

    this.rawContent = raw;
    return result;
  }

  private transient String rawContent;

  @Override
  public String getTextOutput(CommandResult<JsonNode> result) {

    return rawContent;
  }

  private TemplateLocation resolveTemplate() {
    if (EventTemplateCatalog.listAll().isEmpty()) {
      throw new CliException(msg.eventTemplatesNoTemplatesFound());
    }
    String input = templateId;
    if (input == null || input.isBlank()) {
      input = InteractivePicker.pick(
          msg.eventTemplateGetPrompt(), EventTemplateCatalog.templateIds());
      if (input == null || input.isBlank()) {
        throw new CliException(msg.eventTemplateIdRequired());
      }
    }
    final String resolved = input;
    return EventTemplateCatalog.findById(resolved)
        .orElseThrow(() -> new CliException(msg.eventTemplateNotFound(resolved)));
  }
}
