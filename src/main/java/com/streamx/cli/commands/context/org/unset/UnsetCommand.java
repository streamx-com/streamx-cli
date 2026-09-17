package com.streamx.cli.commands.context.org.unset;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import java.io.IOException;
import picocli.CommandLine;

@CommandLine.Command(
    name = "unset",
    header = "Clear the current organization of the active context",
    description = "Also clears the current project (it cannot exist without an organization). "
        + "Idempotent. A STREAMX_ORG environment variable is not affected."
)
public class UnsetCommand extends AbstractSilentCommand {

  @Override
  public CommandResult<Void> runCommand() {
    try {
      final boolean hadProject = StreamxHome.readCurrentProject() != null;
      StreamxHome.clearCurrentOrg();
      StreamxHome.clearCurrentProject();
      System.out.println(msg.orgUnset());
      if (hadProject) {
        System.out.println(msg.projectUnset());
      }
    } catch (IOException e) {
      throw new CliException(e.getMessage(), e);
    }
    return new CommandResult<>(null);
  }
}
