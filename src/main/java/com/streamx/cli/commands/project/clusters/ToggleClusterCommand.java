package com.streamx.cli.commands.project.clusters;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrganizationClustersApi;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;

public abstract class ToggleClusterCommand extends AbstractSilentCommand {

  protected CommandResult<Void> stage(ProjectScopedOptions scope, String clusterId,
      boolean enable) {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    String project = context.project();
    try (PlatformClients client = PlatformClients.fromConfig()) {
      new OrganizationClustersApi(client).setClusterEnabled(context.org(), project, clusterId,
          enable);
    }
    System.err.println(enable ? msg.projectClusterEnabled(clusterId, project)
        : msg.projectClusterDisabled(clusterId, project));
    return new CommandResult<>(null);
  }
}
