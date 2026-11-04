package com.streamx.cli.commands.project.domains.get;

import com.streamx.cli.commands.project.domains.DomainsSupport;
import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectDomainView;
import com.streamx.cli.platform.ProjectDomainsApi;
import picocli.CommandLine;

@CommandLine.Command(
    name = "get",
    header = "Show a custom domain's verification state and the DNS records it needs"
)
public class GetCommand extends AbstractCommand<ProjectDomainView> {

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "<domain>",
      description = "Domain ID or hostname, as shown by 'streamx project domains list'"
  )
  public String idOrHostname;

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @Override
  public String getTextOutput(CommandResult<ProjectDomainView> result) {
    return DomainsSupport.details(result.getData());
  }

  @Override
  public CommandResult<ProjectDomainView> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      return new CommandResult<>(DomainsSupport.require(
          new ProjectDomainsApi(client).list(context.org(), context.project()),
          DomainsSupport.normalize(idOrHostname),
          context.project()));
    }
  }
}
