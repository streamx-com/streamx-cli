package com.streamx.cli.commands.context.project.unset;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import java.io.IOException;
import picocli.CommandLine;

@CommandLine.Command(
    name = "unset",
    header = "Clear the current project of the active context",
    description = "The current organization is kept. Idempotent. A STREAMX_PROJECT "
        + "environment variable is not affected."
)
public class UnsetCommand extends AbstractSilentCommand {

  @Override
  public CommandResult<Void> runCommand() {
    try {
      StreamxHome.clearCurrentProject();
    } catch (IOException e) {
      throw new CliException(e.getMessage(), e);
    }
    System.out.println(msg.projectUnset());
    return new CommandResult<>(null);
  }
}
