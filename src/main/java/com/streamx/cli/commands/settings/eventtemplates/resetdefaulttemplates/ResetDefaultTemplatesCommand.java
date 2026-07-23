package com.streamx.cli.commands.settings.eventtemplates.resetdefaulttemplates;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.publish.event.DefaultEventTemplates;
import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.InteractivePicker;
import com.streamx.cli.framework.PathUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import picocli.CommandLine;

@CommandLine.Command(
    name = "reset-default-templates",
    header = "Delete and repopulate the <streamxHome>/default-event-templates folder",
    description = "Wipes the bundled default templates from <streamxHome>/default-event-templates "
        + "(shared by all profiles) and restores them from the files embedded in the CLI jar. "
        + "The profiles' own event templates and registered templates in settings are not "
        + "touched.",
    footer = {
        "",
        "Examples:",
        "  streamx settings event-templates reset-default-templates",
        "  streamx settings event-templates reset-default-templates --yes   # skip confirmation"
    }
)
public class ResetDefaultTemplatesCommand
    extends AbstractCommand<ResetDefaultTemplatesCommandResult> {

  @CommandLine.Option(
      names = {"-y", "--yes"},
      description = "Skip the confirmation prompt (required in non-interactive environments)"
  )
  public boolean yes;

  @Override
  public CommandResult<ResetDefaultTemplatesCommandResult> runCommand() {
    Path dir = StreamxHome.getStreamxHome().resolve(DefaultEventTemplates.DIRECTORY);

    if (!yes) {

      String prompt = msg.eventTemplatesResetConfirm(dir.toAbsolutePath().toString());
      String answer = InteractivePicker.pick(prompt, null);
      if (answer == null || !isYes(answer)) {
        throw new CliException(msg.eventTemplatesResetCancelled());
      }
    }

    PathUtils.deleteRecursivelyIfExists(dir);
    DefaultEventTemplates.populate();

    if (!Files.isDirectory(dir)) {
      throw new CliException(msg.eventTemplatesResetFailed(dir.toAbsolutePath().toString()));
    }

    List<String> restored = listRestoredTemplateIds(dir);

    return new CommandResult<>(new ResetDefaultTemplatesCommandResult(
        dir.toAbsolutePath().toString(),
        restored
    ));
  }

  @Override
  public String getTextOutput(CommandResult<ResetDefaultTemplatesCommandResult> result) {
    ResetDefaultTemplatesCommandResult data = result.getData();
    return msg.eventTemplatesResetSucceeded(data.path(), data.templates().size());
  }

  private static boolean isYes(String answer) {
    String s = answer.strip().toLowerCase();
    return "y".equals(s) || "yes".equals(s);
  }

  private static List<String> listRestoredTemplateIds(Path dir) {
    try (Stream<Path> stream = Files.list(dir)) {
      return stream
          .filter(Files::isRegularFile)
          .map(p -> p.getFileName().toString())
          .filter(name -> name.endsWith(DefaultEventTemplates.EXTENSION))
          .map(name -> name.substring(
              0, name.length() - DefaultEventTemplates.EXTENSION.length()))
          .sorted()
          .toList();
    } catch (java.io.IOException e) {
      throw new CliException(
          msg.failedToListEventTemplates(dir.toAbsolutePath().toString(), e.getMessage()), e);
    }
  }
}
