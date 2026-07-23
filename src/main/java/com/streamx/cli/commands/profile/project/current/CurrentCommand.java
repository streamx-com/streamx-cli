package com.streamx.cli.commands.profile.project.current;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformContext;
import picocli.CommandLine;

@CommandLine.Command(
    name = "current",
    header = "Print the current project",
    description = "The effective value: STREAMX_PROJECT if set, otherwise the active "
        + "profile's current-project."
)
public class CurrentCommand extends AbstractSilentCommand {

  @Override
  public CommandResult<Void> runCommand() {
    String project = PlatformContext.effectiveProject();
    if (project == null) {
      throw new CliException(msg.noCurrentProject());
    }
    System.out.println(project);
    return new CommandResult<>(null);
  }
}
