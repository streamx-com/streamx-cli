package com.streamx.cli.commands.completion;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.Cluster;
import com.streamx.cli.platform.OrganizationClustersApi;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import java.util.List;
import java.util.Objects;
import picocli.CommandLine;

@CommandLine.Command(
    name = "__complete-cluster-ids",
    hidden = true,
    header = "Internal: list cluster IDs available to an organization for shell completion"
)
public class CompleteClusterIdsCommand extends AbstractCommand<List<String>> {

  @CommandLine.Parameters(index = "0", arity = "0..1", description = "Organization ID")
  public String orgId;

  @Override
  public CommandResult<List<String>> runCommand() {
    String org = orgId == null || orgId.isBlank() || orgId.startsWith("-")
        ? PlatformContext.effectiveOrg()
        : orgId;
    if (org == null) {
      return new CommandResult<>(List.of());
    }
    try (PlatformClients client = PlatformClients.completion()) {
      return new CommandResult<>(new OrganizationClustersApi(client).list(org).stream()
          .map(Cluster::id)
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
