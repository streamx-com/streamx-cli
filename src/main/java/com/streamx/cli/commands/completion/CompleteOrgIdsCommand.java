package com.streamx.cli.commands.completion;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.Organization;
import com.streamx.cli.platform.OrganizationsApi;
import com.streamx.cli.platform.PlatformApiClient;
import java.util.List;
import java.util.Objects;
import picocli.CommandLine;

@CommandLine.Command(
    name = "__complete-org-ids",
    hidden = true,
    header = "Internal: list organization IDs for shell completion, one per line"
)
public class CompleteOrgIdsCommand extends AbstractCommand<List<String>> {

  @Override
  public CommandResult<List<String>> runCommand() {
    // Shell completion must stay silent and fast on ANY failure (not logged in, platform
    // unreachable, expired session) - an empty list simply completes nothing.
    try (PlatformApiClient client = PlatformApiClient.completionClient()) {
      return new CommandResult<>(new OrganizationsApi(client).list().stream()
          .map(Organization::id)
          .filter(Objects::nonNull)
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
