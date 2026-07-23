package com.streamx.cli.commands.completion;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrganizationUsersApi;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.User;
import java.util.List;
import java.util.Objects;
import picocli.CommandLine;

@CommandLine.Command(
    name = "__complete-org-member-ids",
    hidden = true,
    header = "Internal: list ACTIVE member IDs of an organization for shell completion"
)
public class CompleteOrgMemberIdsCommand extends AbstractCommand<List<String>> {

  @CommandLine.Parameters(index = "0", arity = "0..1", description = "Organization ID")
  public String orgId;

  @Override
  public CommandResult<List<String>> runCommand() {
    // Shell completion must stay silent and fast on ANY failure - an empty list simply
    // completes nothing.
    String org = orgId == null || orgId.isBlank() || orgId.startsWith("-")
        ? PlatformContext.effectiveOrg()
        : orgId;
    if (org == null) {
      return new CommandResult<>(List.of());
    }
    try (PlatformApiClient client = PlatformApiClient.completionClient()) {
      return new CommandResult<>(new OrganizationUsersApi(client).list(org).stream()
          .filter(User::isActive)
          .map(User::id)
          .filter(Objects::nonNull)
          .sorted()
          .toList());
    } catch (RuntimeException anyFailure) {
      return new CommandResult<>(List.of());
    }
  }

  @Override
  public String getTextOutput(CommandResult<List<String>> result) {
    return String.join("\n", result.getData());
  }
}
