package com.streamx.cli.commands.project.domains.status;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.domains.DomainsSupport;
import com.streamx.cli.commands.project.domains.DurationConverter;
import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.TextTable;
import com.streamx.cli.platform.DnsRecordCheckView;
import com.streamx.cli.platform.DomainStatusView;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectDomainView;
import com.streamx.cli.platform.ProjectDomainsApi;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import picocli.CommandLine;

@CommandLine.Command(
    name = "status",
    header = "Show required DNS records and current DNS and HTTPS status",
    description = {
        "Show the required DNS records and check DNS verification and HTTPS readiness. "
            + "Exits with code 0 when the domain is "
            + "ready, or 1 otherwise. Use --watch to keep checking until it is ready."
    },
    footer = {
        "",
        "Examples:",
        "  streamx project domains status shop.example.com",
        "  streamx project domains status shop.example.com --watch --interval 30s"
    }
)
public class StatusCommand extends AbstractCommand<DomainStatusView> {

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "<domain>",
      description = "Domain ID or domain name"
  )
  public String idOrDomain;

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @CommandLine.Option(
      names = "--watch",
      description = "Repeat checks until HTTPS is ready. Press Ctrl+C to stop."
  )
  public boolean watch;

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
    List<String> lines = new ArrayList<>();
    lines.add(msg.domainStatusHeader(status.domain(),
        DomainsSupport.describeStatus(status.verification()),
        DomainsSupport.describeStatus(status.tls()), status.checkedAt()));

    lines.add(TextTable.render(
        List.of("RECORD", "TYPE", "NAME", "REQUIRED VALUE", "STATUS"),
        status.records().stream()
            .map(record -> Arrays.asList(
                getPurpose(record), record.type(), record.name(), formatExpected(record),
                DomainsSupport.describeStatus(record.status())))
            .toList()));

    List<String> details = status.records().stream()
        .filter(record -> !record.inPlace())
        .map(StatusCommand::getDetails)
        .toList();

    if (!details.isEmpty()) {
      lines.add("");
      lines.addAll(details);
    }

    return String.join(System.lineSeparator(), lines);
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
        DomainStatusView status = api.status(context.org(), context.project(), domain);
        CommandResult<DomainStatusView> result = new CommandResult<>(status);

        if (status.ready()) {
          System.err.println(msg.domainReadyForTraffic(domain));
          return result;
        }
        if (!watch) {
          result.setError(new CliException(nextStep(status)));
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

    if ("FAILED".equals(status.verification())) {
      return msg.domainVerificationRetry(domain);
    }

    if (!"VERIFIED".equals(status.verification())) {
      return msg.domainNotVerifiedYet(domain);
    }

    return switch (String.valueOf(status.tls())) {
      case "DOMAIN_NOT_IN_MESH_ROUTES" -> msg.domainNotInMeshRoutes(domain);
      case "HTTPS_NOT_READY" -> msg.domainHttpsNotReady(domain);
      case "CERTIFICATE_DNS_RECORD_PROBLEM" -> msg.domainCertificateRecordProblem(domain);
      case "INVALID_CERTIFICATE" -> msg.domainInvalidCertificate(domain);
      default -> msg.domainHttpsCheckFailed(domain);
    };
  }

  private static String getPurpose(DnsRecordCheckView record) {
    return record.purpose() == null ? null : record.purpose().toLowerCase(Locale.ROOT);
  }

  private static String formatExpected(DnsRecordCheckView record) {
    return record.expected().isEmpty() ? "-" : String.join(" or ", record.expected());
  }

  private static String getDetails(DnsRecordCheckView record) {
    String expected = formatExpected(record);

    return switch (String.valueOf(record.status())) {
      case "MISSING" -> msg.domainRecordMissing(
          getPurpose(record), record.type(), record.name(), expected);
      case "MISMATCH" -> msg.domainRecordMismatch(
          getPurpose(record), record.name(), String.join(", ", record.found()), expected);
      default -> msg.domainRecordLookupFailed(getPurpose(record), record.name());
    };
  }
}
