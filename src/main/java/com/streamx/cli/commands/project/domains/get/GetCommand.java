package com.streamx.cli.commands.project.domains.get;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.domains.DomainsSupport;
import com.streamx.cli.commands.project.domains.DurationConverter;
import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.DetailsView;
import com.streamx.cli.framework.TextList;
import com.streamx.cli.platform.DnsRecordCheckView;
import com.streamx.cli.platform.DomainCompletionCandidates;
import com.streamx.cli.platform.DomainStatusView;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectDomainView;
import com.streamx.cli.platform.ProjectDomainsApi;
import com.streamx.cli.platform.generated.model.DomainStatus.DnsChecksEnum;
import com.streamx.cli.platform.generated.model.DomainStatusRecordsInner.StatusEnum;
import java.time.Duration;
import picocli.CommandLine;

@CommandLine.Command(
    name = "get",
    header = "Show domain setup details and check results",
    description = {
        "Show the required DNS records and check DNS records and TLS certificate readiness. "
            + "Incomplete setup succeeds unless --fail-on-incomplete-setup is set. "
            + "Use --watch to keep checking until setup is complete."
    },
    footer = {
        "",
        "Examples:",
        "  streamx project domains get shop.example.com",
        "  streamx project domains get shop.example.com --watch --interval 30s"
    }
)
public class GetCommand extends AbstractCommand<DomainStatusView> {

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "<domain>",
      completionCandidates = DomainCompletionCandidates.class,
      description = "Domain ID or domain name"
  )
  public String idOrDomain;

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @CommandLine.Option(
      names = "--watch",
      description = "Repeat checks until domain setup is complete. Press Ctrl+C to stop."
  )
  public boolean watch;

  @CommandLine.Option(
      names = "--fail-on-incomplete-setup",
      description = "Exit with code 1 if DNS checks or the TLS certificate are not ready"
  )
  public boolean failOnIncompleteSetup;

  @CommandLine.Option(
      names = "--interval",
      paramLabel = "<duration>",
      defaultValue = "10s",
      converter = DurationConverter.class,
      description = "How often --watch checks, such as 10s or 1m (default: ${DEFAULT-VALUE})"
  )
  public Duration interval;

  @Override
  public String getTextOutput(CommandResult<DomainStatusView> result) {
    DomainStatusView status = result.getData();
    String summary = new DetailsView()
        .row("Domain ID", status.id())
        .row("Domain name", status.domain())
        .row("DNS checks", DomainsSupport.describeDnsChecks(status.dnsChecks()))
        .row("TLS certificate", DomainsSupport.describeTlsCertificate(status.tlsCertificate()))
        .row("Last checked", status.checkedAt())
        .render();
    TextList records = new TextList();
    for (DnsRecordCheckView record : status.records()) {
      DetailsView details = new DetailsView()
          .row("Status", DomainsSupport.describeDnsRecord(record.status()))
          .row("Record type", record.type() == null ? null : record.type().toString())
          .row("Name", record.name())
          .row("Expected value", formatExpected(record));
      if (record.status() == StatusEnum.DNS_RECORD_VALUE_MISMATCH
          && !record.found().isEmpty()) {
        details.row("Found value", String.join(", ", record.found()));
      }
      records.item(recordTitle(record), details);
    }
    String recordList = records.render();
    return recordList.isEmpty() ? summary
        : summary + System.lineSeparator().repeat(2) + recordList;
  }

  @Override
  public CommandResult<DomainStatusView> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      ProjectDomainsApi api = new ProjectDomainsApi(client);
      ProjectDomainView configured = DomainsSupport.requireDomain(
          api.list(context.org(), context.project()),
          DomainsSupport.normalizeDomain(idOrDomain), context.project());
      String domain = configured.domain();
      Duration pause = DomainsSupport.resolvePollInterval(interval);

      while (true) {
        DomainStatusView status = api.status(context.org(), context.project(), configured);
        CommandResult<DomainStatusView> result = new CommandResult<>(status);

        if (status.isSetupComplete()) {
          System.err.println(msg.domainTlsCertificateReady(domain));
          return result;
        }
        if (!watch) {
          if (failOnIncompleteSetup) {
            result.setError(new CliException(nextStep(status)));
          }
          return result;
        }

        // Earlier rounds are progress: stdout keeps a single result, also for -o json or yaml.
        System.err.println(getTextOutput(result));
        System.err.println();

        DomainsSupport.sleep(pause);
      }
    }
  }

  private static String nextStep(DomainStatusView status) {
    String domain = status.domain();

    if (status.dnsChecks() == DnsChecksEnum.FAILED) {
      return msg.domainDnsChecksRetry(domain);
    }

    if (status.dnsChecks() != DnsChecksEnum.PASSED) {
      return msg.domainDnsChecksNotPassed(domain);
    }

    if (status.tlsCertificate() == null) {
      return msg.domainTlsCertificateCheckFailed(domain);
    }
    return switch (status.tlsCertificate()) {
      case DOMAIN_NOT_IN_MESH_ROUTES -> msg.domainNotInMeshRoutes(domain);
      case TLS_CERTIFICATE_NOT_READY -> msg.domainTlsCertificateNotReady(domain);
      case ACME_DELEGATION_RECORD_INVALID -> msg.domainAcmeDelegationRecordInvalid(domain);
      case TLS_CERTIFICATE_INVALID -> msg.domainTlsCertificateInvalid(domain);
      case DNS_CHECKS_NOT_PASSED, TLS_CERTIFICATE_READY, TLS_CERTIFICATE_CHECK_FAILED ->
          msg.domainTlsCertificateCheckFailed(domain);
    };
  }

  private static String recordTitle(DnsRecordCheckView record) {
    if (record.purpose() == null) {
      return msg.domainRecordTitle();
    }
    return switch (record.purpose()) {
      case OWNERSHIP_VERIFICATION -> msg.domainOwnershipRecordTitle();
      case ACME_DELEGATION -> msg.domainAcmeRecordTitle();
    };
  }

  private static String formatExpected(DnsRecordCheckView record) {
    return record.expected().isEmpty() ? "-" : String.join(" or ", record.expected());
  }

}
