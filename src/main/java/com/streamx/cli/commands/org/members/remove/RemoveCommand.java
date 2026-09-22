package com.streamx.cli.commands.org.members.remove;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.OrgMemberIdCompletionCandidates;
import com.streamx.cli.platform.OrganizationUsersApi;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.generated.model.User;
import picocli.CommandLine;

@CommandLine.Command(
    name = "remove",
    header = "Remove a member from an organization"
)
public class RemoveCommand extends AbstractSilentCommand {

  @CommandLine.Option(
      names = "--org",
      paramLabel = "<orgId>",
      description = "Organization ID (defaults to the current organization)",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @CommandLine.Parameters(
      index = "0",
      description = "ID of an ACTIVE member, as shown by 'streamx org members list'",
      completionCandidates = OrgMemberIdCompletionCandidates.class
  )
  public String userId;

  @Override
  public CommandResult<Void> runCommand() {
    orgId = PlatformContext.requireOrg(orgId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      OrganizationUsersApi users = new OrganizationUsersApi(client);

      users.requireActiveMember(orgId, userId,
          (id, status) -> msg.orgMemberNotActiveForRemoval(id, status, orgId));

      users.remove(orgId, userId);
    }
    System.err.println(msg.orgMemberRemoved(userId));
    return new CommandResult<>(null);
  }
}
