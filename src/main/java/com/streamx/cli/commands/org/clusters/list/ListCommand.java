package com.streamx.cli.commands.org.clusters.list;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.TextTable;
import com.streamx.cli.platform.Cluster;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.OrganizationClustersApi;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    header = "List clusters available to an organization"
)
public class ListCommand extends AbstractCommand<List<Cluster>> {

  @CommandLine.Option(
      names = "--org",
      paramLabel = "<orgId>",
      description = "Organization ID (defaults to the current organization)",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @CommandLine.Option(
      names = {"-q", "--quiet"},
      description = "Only display cluster IDs, one per line (for piping to xargs)"
  )
  public boolean quiet;

  @Override
  public String getTextOutput(CommandResult<List<Cluster>> result) {
    List<Cluster> clusters = result.getData();

    if (quiet) {
      return clusters.stream()
          .map(Cluster::id)
          .filter(Objects::nonNull)
          .collect(Collectors.joining("\n"));
    }

    if (clusters.isEmpty()) {
      return msg.orgClustersListEmpty();
    }

    return TextTable.render(
        List.of("ID", "TYPE", "NAME", "ENABLED"),
        clusters.stream()
            .map(cluster -> Arrays.asList(
                cluster.id(),
                cluster.type(),
                cluster.name(),
                String.valueOf(cluster.enabled())))
            .toList());
  }

  @Override
  public CommandResult<List<Cluster>> runCommand() {
    orgId = PlatformContext.requireOrg(orgId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      return new CommandResult<>(new OrganizationClustersApi(client).list(orgId));
    }
  }
}
