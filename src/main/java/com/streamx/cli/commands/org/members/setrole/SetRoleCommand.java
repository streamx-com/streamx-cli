package com.streamx.cli.commands.org.members.setrole;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.OrgMemberIdCompletionCandidates;
import com.streamx.cli.platform.OrganizationUsersApi;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.Roles;
import com.streamx.cli.platform.User;
import picocli.CommandLine;

@CommandLine.Command(
    name = "set-role",
    header = "Change the role of an organization member"
)
public class SetRoleCommand extends AbstractSilentCommand {

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

  @CommandLine.Option(
      names = {"-r", "--role"},
      required = true,
      description = "New role: ${COMPLETION-CANDIDATES}",
      completionCandidates = Roles.class
  )
  public String role;

  @Override
  public CommandResult<Void> runCommand() {
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      OrganizationUsersApi users = new OrganizationUsersApi(client);

      // The server implements the role change as remove-then-add, so applying it to a pending
      // invitation listed by 'members list' would grant membership without the invitation ever
      // being accepted. Refuse rather than do that silently.
      User member = users.find(orgId, userId)
          .orElseThrow(() -> new CliException(msg.orgMemberNotFound(userId, orgId)));
      if (!member.isActive()) {
        throw new CliException(
            msg.orgMemberNotActiveForRoleChange(userId, member.status(), orgId, userId));
      }

      users.editRole(orgId, userId, role);
    }
    System.out.println(msg.orgMemberRoleChanged(userId, role));
    return new CommandResult<>(null);
  }
}
