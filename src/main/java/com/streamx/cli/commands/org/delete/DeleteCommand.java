package com.streamx.cli.commands.org.delete;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.DeleteConfirmation;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.OrganizationsApi;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.PlatformContext;
import picocli.CommandLine;

@CommandLine.Command(
    name = "delete",
    header = "Delete an organization",
    description = "Asks to type the organization ID back as confirmation; "
        + "--force deletes without asking."
)
public class DeleteCommand extends AbstractSilentCommand {
  @CommandLine.Parameters(
      index = "0",
      arity = "0..1",
      description = "Organization ID (defaults to the current organization)",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @CommandLine.Option(
      names = {"-f", "--force"},
      description = "Skip the confirmation prompt (required in non-interactive environments)"
  )
  public boolean force;

  @Override
  public CommandResult<Void> runCommand() {
    orgId = PlatformContext.requireOrg(orgId);
    DeleteConfirmation.require(force, orgId);
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      new OrganizationsApi(client).delete(orgId);
    }
    System.out.println(msg.orgDeleted(orgId));
    return new CommandResult<>(null);
  }
}
