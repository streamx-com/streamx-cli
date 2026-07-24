package com.streamx.cli.commands.org.members.list;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.TextTable;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.OrganizationUsersApi;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.generated.model.User;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    header = "List organization members"
)
public class ListCommand extends AbstractCommand<List<User>> {

  @CommandLine.Option(
      names = "--org",
      paramLabel = "<orgId>",
      description = "Organization ID (defaults to the current organization)",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @CommandLine.Option(
      names = {"-q", "--quiet"},
      description = "Only display member IDs, one per line (for piping to xargs)"
  )
  public boolean quiet;

  @Override
  public String getTextOutput(CommandResult<List<User>> result) {
    List<User> users = result.getData();

    if (quiet) {
      return users.stream()
          .map(User::getId)
          .filter(Objects::nonNull)
          .collect(Collectors.joining("\n"));
    }

    if (users.isEmpty()) {
      return msg.orgMembersListEmpty();
    }

    return TextTable.render(
        List.of("ID", "DISPLAY NAME", "ROLE", "STATUS", ""),
        users.stream()
            .map(user -> java.util.Arrays.asList(
                user.getId(),
                user.getDisplayName(),
                user.getRole() == null ? null : user.getRole().getName(),
                user.getStatus() == null ? null : user.getStatus().value(),
                Boolean.TRUE.equals(user.getIsCaller()) ? "(you)" : ""))
            .toList());
  }

  @Override
  public CommandResult<List<User>> runCommand() {
    orgId = PlatformContext.requireOrg(orgId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      return new CommandResult<>(new OrganizationUsersApi(client).list(orgId));
    }
  }
}
