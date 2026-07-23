package com.streamx.cli.commands.org.invitations.list;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.TextTable;
import com.streamx.cli.platform.Invitation;
import com.streamx.cli.platform.OrganizationInvitationsApi;
import com.streamx.cli.platform.PlatformApiClient;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    header = "List pending organization invitations"
)
public class ListCommand extends AbstractCommand<List<Invitation>> {

  @CommandLine.Parameters(index = "0", description = "Organization ID")
  public String orgId;

  @CommandLine.Option(
      names = {"-q", "--quiet"},
      description = "Only display invited emails, one per line (for piping to xargs)"
  )
  public boolean quiet;

  @Override
  public String getTextOutput(CommandResult<List<Invitation>> result) {
    List<Invitation> invitations = result.getData();

    if (quiet) {
      return invitations.stream()
          .map(Invitation::email)
          .filter(Objects::nonNull)
          .collect(Collectors.joining("\n"));
    }

    if (invitations.isEmpty()) {
      return msg.orgInvitationsListEmpty();
    }

    return TextTable.render(
        List.of("EMAIL", "ROLE", "STATUS"),
        invitations.stream()
            .map(invitation -> Arrays.asList(
                invitation.email(), invitation.role(), invitation.status()))
            .toList());
  }

  @Override
  public CommandResult<List<Invitation>> runCommand() {
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      return new CommandResult<>(new OrganizationInvitationsApi(client).list(orgId));
    }
  }
}
