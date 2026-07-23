package com.streamx.cli.commands.org.invitations.cancel;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrganizationInvitationsApi;
import com.streamx.cli.platform.PlatformApiClient;
import picocli.CommandLine;

@CommandLine.Command(
    name = "cancel",
    header = "Cancel a pending organization invitation"
)
public class CancelCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(index = "0", description = "Organization ID")
  public String orgId;

  @CommandLine.Parameters(index = "1", description = "Invited email address")
  public String email;

  @Override
  public CommandResult<Void> runCommand() {
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      new OrganizationInvitationsApi(client).cancel(orgId, email);
    }
    System.out.println(msg.orgInvitationCancelled(email));
    return new CommandResult<>(null);
  }
}
