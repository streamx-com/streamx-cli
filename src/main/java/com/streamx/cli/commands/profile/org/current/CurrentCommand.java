package com.streamx.cli.commands.profile.org.current;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformContext;
import picocli.CommandLine;

@CommandLine.Command(
    name = "current",
    header = "Print the current organization",
    description = "The effective value: STREAMX_ORG if set, otherwise the active profile's "
        + "current-org."
)
public class CurrentCommand extends AbstractSilentCommand {

  @Override
  public CommandResult<Void> runCommand() {
    String org = PlatformContext.effectiveOrg();
    if (org == null) {
      throw new CliException(msg.noCurrentOrg());
    }
    System.out.println(org);
    return new CommandResult<>(null);
  }
}
