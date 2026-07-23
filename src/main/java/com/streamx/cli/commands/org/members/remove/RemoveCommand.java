package com.streamx.cli.commands.org.members.remove;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.OrgMemberIdCompletionCandidates;
import com.streamx.cli.platform.OrganizationUsersApi;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.User;
import picocli.CommandLine;

@CommandLine.Command(
    name = "remove",
    header = "Remove a member from an organization"
)
public class RemoveCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(
      index = "0",
      description = "Organization ID",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @CommandLine.Parameters(
      index = "1",
      description = "ID of an ACTIVE member, as shown by 'streamx org members list'",
      completionCandidates = OrgMemberIdCompletionCandidates.class
  )
  public String userId;

  @Override
  public CommandResult<Void> runCommand() {
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      OrganizationUsersApi users = new OrganizationUsersApi(client);

      // 'members list' also reports pending invitations, and the server rejects removing one.
      // Checked here so the answer names the actual problem instead of surfacing a bare 404.
      User member = users.find(orgId, userId)
          .orElseThrow(() -> new CliException(msg.orgMemberNotFound(userId, orgId)));
      if (!member.isActive()) {
        throw new CliException(
            msg.orgMemberNotActiveForRemoval(userId, member.status(), orgId, userId));
      }

      users.remove(orgId, userId);
    }
    System.out.println(msg.orgMemberRemoved(userId));
    return new CommandResult<>(null);
  }
}
