package com.streamx.cli.commands.org.members.setrole;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.OrgMemberIdCompletionCandidates;
import com.streamx.cli.platform.OrganizationUsersApi;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.Roles;
import com.streamx.cli.platform.generated.model.User;
import picocli.CommandLine;

@CommandLine.Command(
    name = "set-role",
    header = "Change the role of an organization member"
)
public class SetRoleCommand extends AbstractSilentCommand {

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

  @CommandLine.Option(
      names = {"-r", "--role"},
      required = true,
      description = "New role: ${COMPLETION-CANDIDATES}",
      completionCandidates = Roles.class
  )
  public String role;

  @Override
  public CommandResult<Void> runCommand() {
    orgId = PlatformContext.requireOrg(orgId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      OrganizationUsersApi users = new OrganizationUsersApi(client);

      User member = users.find(orgId, userId)
          .orElseThrow(() -> new CliException(msg.orgMemberNotFound(userId, orgId)));
      if (member.getStatus() != User.StatusEnum.ACTIVE) {
        String status = member.getStatus() == null ? "" : member.getStatus().value();
        throw new CliException(
            msg.orgMemberNotActiveForRoleChange(userId, status, orgId, userId));
      }

      users.editRole(orgId, userId, role);
    }
    System.out.println(msg.orgMemberRoleChanged(userId, role));
    return new CommandResult<>(null);
  }
}
