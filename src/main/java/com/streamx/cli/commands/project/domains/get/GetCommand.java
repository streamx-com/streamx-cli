package com.streamx.cli.commands.project.domains.get;

import com.streamx.cli.commands.project.domains.DomainsSupport;
import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.DomainCompletionCandidates;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectDomainView;
import com.streamx.cli.platform.ProjectDomainsApi;
import picocli.CommandLine;

@CommandLine.Command(
    name = "get",
    header = "Show a domain's verification status and required DNS records"
)
public class GetCommand extends AbstractCommand<ProjectDomainView> {

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "<domain>",
      completionCandidates = DomainCompletionCandidates.class,
      description = "Domain ID or domain name"
  )
  public String idOrDomain;

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @Override
  public String getTextOutput(CommandResult<ProjectDomainView> result) {
    return DomainsSupport.formatDomainDetails(result.getData());
  }

  @Override
  public CommandResult<ProjectDomainView> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      return new CommandResult<>(DomainsSupport.requireDomain(
          new ProjectDomainsApi(client).list(context.org(), context.project()),
          DomainsSupport.normalizeDomain(idOrDomain),
          context.project()));
    }
  }
}
