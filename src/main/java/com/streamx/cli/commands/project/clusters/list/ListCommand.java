package com.streamx.cli.commands.project.clusters.list;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.TextTable;
import com.streamx.cli.platform.Cluster;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.OrganizationClustersApi;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectIdCompletionCandidates;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    header = "List clusters enabled for the project in the live configuration"
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
      names = "--project",
      paramLabel = "<projectId>",
      description = "Project ID (defaults to the current project)",
      completionCandidates = ProjectIdCompletionCandidates.class
  )
  public String projectId;

  @CommandLine.Option(
      names = {"-q", "--quiet"},
      description = "Only display cluster IDs, one per line (for piping to xargs)"
  )
  public boolean quiet;

  @CommandLine.Option(
      names = "--all",
      description = "List all clusters available to the project"
  )
  public boolean all;

  @Override
  public String getTextOutput(CommandResult<List<Cluster>> result) {
    List<Cluster> clusters = result.getData();

    if (quiet) {
      return clusters.stream()
          .map(Cluster::id)
          .collect(Collectors.joining("\n"));
    }

    if (clusters.isEmpty()) {
      return msg.orgClustersListEmpty();
    }

    return TextTable.render(
        List.of("ID", "TYPE", "NAME"),
        clusters.stream()
            .map(cluster -> Arrays.asList(
                cluster.id(),
                cluster.type(),
                cluster.name()))
            .toList());
  }

  @Override
  public CommandResult<List<Cluster>> runCommand() {
    PlatformContext.OrgProject context = PlatformContext.orgAndProject(orgId, projectId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      OrganizationClustersApi api = new OrganizationClustersApi(client);
      List<Cluster> clusters = all
          ? api.listAvailableProjectClusters(context.org(), context.project())
          : api.listEnabledProjectClusters(context.org(), context.project());
      return new CommandResult<>(clusters);
    }
  }
}
