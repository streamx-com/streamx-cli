package com.streamx.cli.commands.org.invitations.cancel;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.InvitedEmailCompletionCandidates;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.OrganizationInvitationsApi;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.PlatformContext;
import picocli.CommandLine;

@CommandLine.Command(
    name = "cancel",
    header = "Cancel a pending organization invitation"
)
public class CancelCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(
      index = "0",
      arity = "0..1",
      description = "Organization ID (defaults to the current organization)",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @CommandLine.Parameters(
      index = "1",
      arity = "0..1",
      description = "Invited email address",
      completionCandidates = InvitedEmailCompletionCandidates.class
  )
  public String email;

  @Override
  public CommandResult<Void> runCommand() {
    PlatformContext.OrgValue context = PlatformContext.orgAndValue(orgId, email, "email");
    orgId = context.org();
    email = context.value();
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      new OrganizationInvitationsApi(client).cancel(orgId, email);
    }
    System.out.println(msg.orgInvitationCancelled(email));
    return new CommandResult<>(null);
  }
}
