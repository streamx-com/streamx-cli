package com.streamx.cli.commands.project.clusters.set;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.Cluster;
import com.streamx.cli.platform.ClusterIdCompletionCandidates;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.OrganizationClustersApi;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectIdCompletionCandidates;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "set",
    header = "Stage the full list of enabled clusters",
    description = "Use 'streamx project deployment push' to publish the selection."
)
public class SetCommand extends AbstractSilentCommand {

  @CommandLine.ArgGroup(multiplicity = "1")
  Selection selection;

  static class Selection {

    @CommandLine.Parameters(
        index = "0",
        arity = "1",
        split = ",",
        paramLabel = "<clusterId>",
        description = "Comma-separated cluster IDs, as shown by 'streamx org clusters list'",
        completionCandidates = ClusterIdCompletionCandidates.class
    )
    List<String> clusterIds;

    @CommandLine.Option(names = "--clear", description = "Disable all clusters")
    boolean clear;
  }

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

  @Override
  public CommandResult<Void> runCommand() {
    PlatformContext.OrgProject context = PlatformContext.orgAndProject(orgId, projectId);
    List<String> clusterIds = selection.clear ? List.of() : selection.clusterIds;
    try (PlatformClients client = PlatformClients.fromConfig()) {
      OrganizationClustersApi clusters = new OrganizationClustersApi(client);

      if (!selection.clear) {
        Set<String> available = clusters
            .listAvailableProjectClusters(context.org(), context.project()).stream()
            .map(Cluster::id)
            .collect(Collectors.toSet());
        for (String clusterId : clusterIds) {
          if (!available.contains(clusterId)) {
            throw new CliException(msg.projectClusterUnknown(
                clusterId, available.stream().sorted().collect(Collectors.joining(", "))));
          }
        }
      }

      clusters.setForProject(context.org(), context.project(), clusterIds);
    }
    System.err.println(selection.clear ? msg.projectClustersCleared(context.project())
        : msg.projectClustersSet(context.project(), String.join(", ", clusterIds)));
    return new CommandResult<>(null);
  }
}
