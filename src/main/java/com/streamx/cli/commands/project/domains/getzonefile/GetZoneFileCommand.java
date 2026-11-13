package com.streamx.cli.commands.project.domains.getzonefile;

import com.streamx.cli.commands.project.domains.DomainsSupport;
import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.CommonOptions;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectDomainView;
import com.streamx.cli.platform.ProjectDomainsApi;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "get-zone-file",
    header = "Print required DNS records in zone-file format",
    description = "Export the required DNS records without running DNS or HTTPS checks.",
    footer = {
        "",
        "Examples:",
        "  streamx project domains get-zone-file > domains.zone",
        "  streamx project domains get-zone-file shop",
        "  streamx project domains get-zone-file shop.example.com > shop.zone"
    }
)
public class GetZoneFileCommand extends AbstractCommand<String> {

  @CommandLine.Parameters(
      index = "0",
      arity = "0..1",
      paramLabel = "<domain>",
      description = "Domain ID or domain name. Defaults to all domains in the project."
  )
  public String idOrDomain;

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @Override
  public List<String> getHiddenOptions() {
    return List.of(CommonOptions.OUTPUT_LONG);
  }

  @Override
  public String getTextOutput(CommandResult<String> result) {
    return result.getData();
  }

  @Override
  public CommandResult<String> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);

    try (PlatformClients client = PlatformClients.fromConfig()) {
      List<ProjectDomainView> domains =
          new ProjectDomainsApi(client).list(context.org(), context.project());

      if (idOrDomain != null) {
        domains = List.of(DomainsSupport.requireDomain(domains,
            DomainsSupport.normalizeDomain(idOrDomain), context.project()));
      }

      return new CommandResult<>(DomainsSupport.formatDnsZoneFile(domains.stream()
          .flatMap(domain -> domain.dnsRecords().stream())
          .toList()));
    }
  }
}
