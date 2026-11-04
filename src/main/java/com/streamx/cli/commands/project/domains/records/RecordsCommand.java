package com.streamx.cli.commands.project.domains.records;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.domains.DomainsSupport;
import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.OutputFormat;
import com.streamx.cli.platform.DnsRecordView;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectDomainView;
import com.streamx.cli.platform.ProjectDomainsApi;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "records",
    header = "Print the DNS records that custom domains need",
    footer = {
        "",
        "Examples:",
        "  streamx project domains records",
        "  streamx project domains records shop.example.com --zone"
    }
)
public class RecordsCommand extends AbstractCommand<List<DnsRecordView>> {

  @CommandLine.Parameters(
      index = "0",
      arity = "0..1",
      paramLabel = "<domain>",
      description = "Only this domain, by ID or hostname (defaults to all domains of the project)"
  )
  public String idOrHostname;

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @CommandLine.Option(
      names = "--zone",
      description = "Print zone-file lines with absolute names and no TTL"
  )
  public boolean zone;

  @Override
  public String getTextOutput(CommandResult<List<DnsRecordView>> result) {
    List<DnsRecordView> records = result.getData();
    if (zone) {
      return DomainsSupport.zoneLines(records);
    }
    if (records.isEmpty()) {
      return msg.domainsListEmpty();
    }
    return DomainsSupport.recordsTable(records);
  }

  @Override
  public CommandResult<List<DnsRecordView>> runCommand() {
    if (zone && output != OutputFormat.text) {
      throw new CommandLine.ParameterException(spec.commandLine(), msg.domainsZoneNeedsText());
    }
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      List<ProjectDomainView> domains =
          new ProjectDomainsApi(client).list(context.org(), context.project());
      if (idOrHostname != null) {
        domains = List.of(DomainsSupport.require(
            domains, DomainsSupport.normalize(idOrHostname), context.project()));
      }
      return new CommandResult<>(domains.stream()
          .flatMap(domain -> domain.dnsRecords().stream())
          .toList());
    }
  }
}
