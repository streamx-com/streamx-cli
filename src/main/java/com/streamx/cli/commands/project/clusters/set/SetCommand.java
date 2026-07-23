package com.streamx.cli.commands.project.clusters.set;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.Cluster;
import com.streamx.cli.platform.ClusterIdCompletionCandidates;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.OrganizationClustersApi;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectIdCompletionCandidates;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "set",
    header = "Set the full list of clusters a project runs on",
    description = "Replaces the project's enabled clusters with exactly the given IDs."
)
public class SetCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(
      index = "0..*",
      arity = "1..*",
      paramLabel = "<clusterId>",
      description = "Cluster IDs, as shown by 'streamx org clusters list'",
      completionCandidates = ClusterIdCompletionCandidates.class
  )
  public List<String> clusterIds;

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
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      OrganizationClustersApi clusters = new OrganizationClustersApi(client);

      Set<String> available = clusters.listForProject(context.org(), context.project()).stream()
          .map(Cluster::id)
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
      for (String clusterId : clusterIds) {
        if (!available.contains(clusterId)) {
          throw new CliException(msg.projectClusterUnknown(
              clusterId, available.stream().sorted().collect(Collectors.joining(", "))));
        }
      }

      clusters.setForProject(context.org(), context.project(), clusterIds);
    }
    System.out.println(
        msg.projectClustersSet(context.project(), String.join(", ", clusterIds)));
    return new CommandResult<>(null);
  }
}
