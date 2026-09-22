package com.streamx.cli.commands.context.org.current;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformContext;
import picocli.CommandLine;

@CommandLine.Command(
    name = "current",
    header = "Print the current organization",
    description = "The effective value: STREAMX_ORG if set, otherwise the active context's "
        + "current-org."
)
public class CurrentCommand extends AbstractCommand<String> {

  @Override
  public CommandResult<String> runCommand() {
    String org = PlatformContext.effectiveOrg();
    if (org == null) {
      throw new CliException(msg.noCurrentOrg());
    }
    return new CommandResult<>(org);
  }

  @Override
  public String getTextOutput(CommandResult<String> result) {
    return result.getData();
  }
}
