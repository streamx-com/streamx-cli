package com.streamx.cli.commands.settings.eventtemplates.edit;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import com.streamx.cli.commands.publish.event.EventTemplateCatalog.TemplateLocation;
import com.streamx.cli.commands.publish.event.UserEventTemplates;
import com.streamx.cli.commands.settings.eventtemplates.TemplateIdCompletionCandidates;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.InteractivePicker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import picocli.CommandLine;

@CommandLine.Command(
    name = "edit",
    header = "Open an event template in $EDITOR",
    description = {
        "Default templates are copied into the context's event-templates/ folder before editing.",
        "On save, the file is re-validated as JSON; invalid JSON re-opens the editor."
    },
    footer = {
        "",
        "Examples:",
        "  streamx settings event-templates edit page.published",
        "  streamx settings event-templates edit   # picks interactively",
        "  EDITOR=code streamx settings event-templates edit my.custom"
    }
)
public class EditCommand extends AbstractCommand<EditCommandResult> {

  public static final String EDITOR = "EDITOR";

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String ERROR_LINE_PREFIX = "//";

  @CommandLine.Parameters(
      index = "0",
      arity = "0..1",
      description = "Template ID (prompts if omitted)",
      completionCandidates = TemplateIdCompletionCandidates.class
  )
  public String templateId;

  @Override
  public CommandResult<EditCommandResult> runCommand() {
    TemplateLocation found = resolveTemplate();

    Path target = EventTemplateCatalog.SOURCE_DEFAULT.equals(found.source())
        ? copyDefaultIntoUserFolder(found)
        : Path.of(found.path());

    String editor = resolveEditor();
    launchEditorAndValidateLoop(editor, target);

    return new CommandResult<>(new EditCommandResult(
        found.id(), target.toAbsolutePath().toString(), editor));
  }

  @Override
  public String getTextOutput(CommandResult<EditCommandResult> result) {
    EditCommandResult data = result.getData();
    return msg.eventTemplateEdited(data.id(), data.path());
  }

  private TemplateLocation resolveTemplate() {
    if (EventTemplateCatalog.listAll().isEmpty()) {
      throw new CliException(msg.eventTemplatesNoTemplatesFound());
    }
    String input = templateId;
    if (input == null || input.isBlank()) {
      input = InteractivePicker.pick(
          msg.eventTemplateEditPrompt(), EventTemplateCatalog.templateIds());
      if (input == null || input.isBlank()) {
        throw new CliException(msg.eventTemplateIdRequired());
      }
    }
    final String resolved = input;
    return EventTemplateCatalog.findById(resolved)
        .orElseThrow(() -> new CliException(msg.eventTemplateNotFound(resolved)));
  }

  private Path copyDefaultIntoUserFolder(TemplateLocation found) {
    Path target = UserEventTemplates.resolve(found.id());
    if (Files.exists(target)) {
      return target;
    }
    try {
      Files.createDirectories(target.getParent());
      Files.copy(Path.of(found.path()), target);
    } catch (IOException e) {
      throw new CliException(
          msg.failedToCopyEventTemplate(
              found.path(), target.toAbsolutePath().toString(), e.getMessage()),
          e);
    }
    return target;
  }

  private void launchEditorAndValidateLoop(String editor, Path target) {
    while (true) {
      int exitCode = launchEditor(editor, target);
      if (exitCode != 0) {
        throw new CliException(msg.editorExitedWithError(editor, exitCode));
      }

      stripLeadingErrorBanner(target);

      String parseError = validateJson(target);
      if (parseError == null) {
        return;
      }

      prependErrorBanner(target, parseError);

      System.err.println(msg.eventTemplateEditInvalidJson(
          target.toAbsolutePath().toString(), parseError));
      System.err.println(msg.eventTemplateEditReopening(editor));
    }
  }

  private void stripLeadingErrorBanner(Path file) {
    try {
      String content = Files.readString(file);
      String[] lines = content.split("\\R", -1);
      int idx = 0;
      while (idx < lines.length && lines[idx].startsWith(ERROR_LINE_PREFIX)) {
        idx++;
      }
      if (idx > 0) {
        String stripped = String.join("\n",
            java.util.Arrays.copyOfRange(lines, idx, lines.length));
        Files.writeString(file, stripped);
      }
    } catch (IOException ignored) {
      // best-effort
    }
  }

  private void prependErrorBanner(Path file, String parseError) {
    try {
      String content = Files.readString(file);
      StringBuilder banner = new StringBuilder();
      banner.append("// ").append(msg.eventTemplateEditErrorBannerHeader()).append('\n');
      for (String line : parseError.split("\\R")) {
        banner.append("// ").append(line).append('\n');
      }
      Files.writeString(file, banner.toString() + content);
    } catch (IOException ignored) {
      // best-effort
    }
  }

  private int launchEditor(String editor, Path target) {
    try {
      Process process = new ProcessBuilder(editor, target.toAbsolutePath().toString())
          .inheritIO()
          .start();
      return process.waitFor();
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new CliException(msg.failedToLaunchEditor(editor, e.getMessage()), e);
    }
  }

  private String validateJson(Path file) {
    try {
      MAPPER.readTree(Files.readString(file));
      return null;
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  private String resolveEditor() {

    String fromProperty = System.getProperty(EDITOR);
    if (fromProperty != null && !fromProperty.isBlank()) {
      return fromProperty;
    }
    String fromEnv = System.getenv(EDITOR);
    if (fromEnv != null && !fromEnv.isBlank()) {
      return fromEnv;
    }
    return "vi";
  }
}
