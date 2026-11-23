package com.streamx.cli.commands.project.clusters.disable;

import com.streamx.cli.commands.project.ProjectScopedOptions;
import com.streamx.cli.commands.project.clusters.ToggleClusterCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.ClusterIdCompletionCandidates;
import picocli.CommandLine;

@CommandLine.Command(
    name = "disable",
    header = "Stage a cluster to be disabled for a project"
)
public class DisableCommand extends ToggleClusterCommand {

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "<clusterId>",
      description = "Cluster ID, as shown by 'streamx project clusters list'",
      completionCandidates = ClusterIdCompletionCandidates.class
  )
  String clusterId;

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @Override
  public CommandResult<Void> runCommand() {
    return stage(scope, clusterId, false);
  }
}
