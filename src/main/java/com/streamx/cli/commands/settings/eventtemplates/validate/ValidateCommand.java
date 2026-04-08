package com.streamx.cli.commands.settings.eventtemplates.validate;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import com.streamx.cli.commands.publish.event.EventTemplateCatalog.TemplateLocation;
import com.streamx.cli.commands.settings.eventtemplates.TemplateIdCompletionCandidates;
import com.streamx.cli.commands.settings.eventtemplates.validate.ValidateCommandResult.TemplateValidation;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.InteractivePicker;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "validate",
    header = "Validate one or all event templates",
    description = "Checks that a template file is valid JSON and has the required CloudEvents "
        + "fields (specversion, id, source, type). Use without arguments or with --all to "
        + "validate every known template. Exit code is non-zero if any template is invalid.",
    footer = {
        "",
        "Examples:",
        "  streamx settings event-templates validate page.published",
        "  streamx settings event-templates validate --all",
        "  streamx settings event-templates validate   # picks interactively"
    }
)
public class ValidateCommand extends AbstractCommand<ValidateCommandResult> {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final List<String> REQUIRED_FIELDS =
      Arrays.asList("specversion", "id", "source", "type");

  @CommandLine.Parameters(
      index = "0",
      arity = "0..1",
      description = "Template ID to validate (prompts if omitted; ignored with --all)",
      completionCandidates = TemplateIdCompletionCandidates.class
  )
  public String templateId;

  @CommandLine.Option(
      names = {"-a", "--all"},
      description = "Validate every known template"
  )
  public boolean all;

  @Override
  public CommandResult<ValidateCommandResult> runCommand() {
    List<TemplateLocation> toCheck = selectTemplates();

    List<TemplateValidation> results = new ArrayList<>(toCheck.size());
    int validCount = 0;
    int invalidCount = 0;
    for (TemplateLocation t : toCheck) {
      String error = validateOne(t);
      boolean valid = error == null;
      results.add(new TemplateValidation(t.id(), t.path(), valid, error));
      if (valid) {
        validCount++;
      } else {
        invalidCount++;
      }
    }

    CommandResult<ValidateCommandResult> cr = new CommandResult<>(
        new ValidateCommandResult(results, validCount, invalidCount));
    if (invalidCount > 0) {
      cr.setExitCodeOverride(1);
    }
    return cr;
  }

  @Override
  public String getTextOutput(CommandResult<ValidateCommandResult> result) {
    StringBuilder sb = new StringBuilder();
    for (TemplateValidation v : result.getData().results()) {
      if (v.valid()) {
        sb.append(msg.eventTemplateValidOk(v.id())).append('\n');
      } else {
        sb.append(msg.eventTemplateValidFailed(v.id(), v.error())).append('\n');
      }
    }
    return sb.toString().stripTrailing();
  }

  private List<TemplateLocation> selectTemplates() {
    List<TemplateLocation> all0 = EventTemplateCatalog.listAll();
    if (all0.isEmpty()) {
      throw new CliException(msg.eventTemplatesNoTemplatesFound());
    }
    if (all) {
      return all0;
    }
    String input = templateId;
    if (input == null || input.isBlank()) {
      input = InteractivePicker.pick(
          msg.eventTemplateValidatePrompt(), EventTemplateCatalog.templateIds());
      if (input == null || input.isBlank()) {
        throw new CliException(msg.eventTemplateIdRequired());
      }
    }
    final String resolved = input;
    TemplateLocation found = EventTemplateCatalog.findById(resolved)
        .orElseThrow(() -> new CliException(msg.eventTemplateNotFound(resolved)));
    return List.of(found);
  }

  private String validateOne(TemplateLocation t) {
    Path file = Path.of(t.path());
    if (!Files.isRegularFile(file)) {
      return msg.eventTemplateFileMissing(t.path());
    }
    JsonNode root;
    try {
      root = MAPPER.readTree(Files.readString(file));
    } catch (Exception e) {
      return e.getMessage();
    }
    for (String field : REQUIRED_FIELDS) {
      if (!root.hasNonNull(field) || root.get(field).asText().isBlank()) {
        return msg.eventTemplateMissingCloudEventField(field);
      }
    }
    return null;
  }
}
