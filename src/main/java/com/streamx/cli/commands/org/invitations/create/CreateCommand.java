package com.streamx.cli.commands.org.invitations.create;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.OrganizationInvitationsApi;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.Roles;
import picocli.CommandLine;

@CommandLine.Command(
    name = "create",
    header = "Invite a user to an organization"
)
public class CreateCommand extends AbstractSilentCommand {

  @CommandLine.Option(
      names = "--org",
      paramLabel = "<orgId>",
      description = "Organization ID (defaults to the current organization)",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @CommandLine.Parameters(
      index = "0",
      description = "Email address to invite"
  )
  public String email;

  @CommandLine.Option(
      names = {"-r", "--role"},
      required = true,
      description = "Role to grant: ${COMPLETION-CANDIDATES}",
      completionCandidates = Roles.class
  )
  public String role;

  @Override
  public CommandResult<Void> runCommand() {
    orgId = PlatformContext.requireOrg(orgId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      new OrganizationInvitationsApi(client).create(orgId, email, role);
    }
    System.out.println(msg.orgInvitationCreated(email, role));
    return new CommandResult<>(null);
  }
}
