package com.streamx.cli.commands.context.org.use;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.PlatformContext;
import picocli.CommandLine;

@CommandLine.Command(
    name = "use",
    header = "Set the current organization for the active context",
    description = "Commands taking an organization fall back to it when the argument is "
        + "omitted. Stored per context; STREAMX_ORG overrides it for a single invocation."
)
public class UseCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(
      index = "0",
      description = "Organization ID",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @Override
  public CommandResult<Void> runCommand() {
    if (orgId.isBlank()) {
      throw new CliException(msg.noOrgContext());
    }
    String clearedProject = PlatformContext.setCurrentOrg(orgId.strip());
    if (clearedProject != null) {
      System.err.println(msg.orgUseClearedProject(clearedProject));
    }
    System.out.println(msg.orgUseSet(orgId.strip()));
    return new CommandResult<>(null);
  }
}
