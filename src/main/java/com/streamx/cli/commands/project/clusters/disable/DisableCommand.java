package com.streamx.cli.commands.project.clusters.disable;

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
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "disable",
    header = "Disable one cluster for a project"
)
public class DisableCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "<clusterId>",
      description = "Cluster ID, as shown by 'streamx project clusters list'",
      completionCandidates = ClusterIdCompletionCandidates.class
  )
  public String clusterId;

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
      List<Cluster> current = clusters.listForProject(context.org(), context.project());

      Cluster target = current.stream()
          .filter(cluster -> clusterId.equals(cluster.id()))
          .findFirst()
          .orElseThrow(() -> new CliException(msg.projectClusterUnknown(clusterId,
              current.stream().map(Cluster::id).sorted().collect(Collectors.joining(", ")))));
      if (!target.enabled()) {
        System.out.println(msg.projectClusterAlreadyDisabled(clusterId, context.project()));
        return new CommandResult<>(null);
      }

      List<String> enabled = current.stream()
          .filter(Cluster::enabled)
          .map(Cluster::id)
          .filter(id -> !clusterId.equals(id))
          .toList();
      clusters.setForProject(context.org(), context.project(), enabled);
    }
    System.out.println(msg.projectClusterDisabled(clusterId, context.project()));
    return new CommandResult<>(null);
  }
}
