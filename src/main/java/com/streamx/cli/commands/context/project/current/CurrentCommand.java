package com.streamx.cli.commands.context.project.current;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformContext;
import picocli.CommandLine;

@CommandLine.Command(
    name = "current",
    header = "Print the current project",
    description = "The effective value: STREAMX_PROJECT if set, otherwise the active "
        + "context's current-project."
)
public class CurrentCommand extends AbstractCommand<String> {

  @Override
  public String getTextOutput(CommandResult<String> result) {
    return result.getData();
  }

  @Override
  public CommandResult<String> runCommand() {
    String project = PlatformContext.effectiveProject();
    if (project == null) {
      throw new CliException(msg.noCurrentProject());
    }
    return new CommandResult<>(project);
  }
}
