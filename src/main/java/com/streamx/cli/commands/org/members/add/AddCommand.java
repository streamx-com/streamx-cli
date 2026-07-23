package com.streamx.cli.commands.org.members.add;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrganizationUsersApi;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.Roles;
import picocli.CommandLine;

@CommandLine.Command(
    name = "add",
    header = "Add an existing user to an organization",
    description = "The account must already exist in the identity provider; it is looked up by "
        + "email. To bring in a new user, send an invitation instead: "
        + "streamx org invitations create"
)
public class AddCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(index = "0", description = "Organization ID")
  public String orgId;

  @CommandLine.Parameters(index = "1", description = "Email of an existing account")
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
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      new OrganizationUsersApi(client).add(orgId, email, role);
    }
    System.out.println(msg.orgMemberAdded(email, role));
    return new CommandResult<>(null);
  }
}
