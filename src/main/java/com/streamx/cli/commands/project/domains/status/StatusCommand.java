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
import com.streamx.cli.platform.ProjectDomainsApi;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import picocli.CommandLine;

@CommandLine.Command(
    name = "status",
    header = "Show how far the setup of a custom domain is",
    description = {
        "Checks the domain as the platform sees it: its two records (the TXT record that "
            + "proves ownership and the _acme-challenge CNAME for the certificate), and, once "
            + "the domain is verified, its HTTPS setup. Ends with exit code 0 when the domain is "
            + "ready, that is when StreamX serves it over HTTPS, and 1 otherwise. With --watch, "
            + "checks again until it is ready."
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
      description = "Domain ID or hostname, as shown by 'streamx project domains list'"
  )
  public String idOrHostname;

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @CommandLine.Option(
      names = "--watch",
      description = "Check again after each interval until the domain is ready"
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
    lines.add(msg.domainStatusHeader(status.domain(), status.verification(), status.tls(),
        status.checkedAt()));
    lines.add(TextTable.render(
        List.of("RECORD", "TYPE", "NAME", "STATUS"),
        status.records().stream()
            .map(record -> Arrays.asList(
                purpose(record), record.type(), record.name(), record.status()))
            .toList()));
    List<String> details = status.records().stream()
        .filter(record -> !record.inPlace())
        .map(StatusCommand::detail)
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
      String domain = DomainsSupport.require(api.list(context.org(), context.project()),
          DomainsSupport.normalize(idOrHostname), context.project()).domain();
      Duration pause = DomainsSupport.pollInterval(interval);
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
    if (!"VERIFIED".equals(status.verification())) {
      return msg.domainNotVerifiedYet(domain);
    }
    return switch (String.valueOf(status.tls())) {
      case "NO_ROUTE" -> msg.domainVerifiedNextStep(domain);
      case "PENDING" -> msg.domainHttpsPending(domain);
      case "RECORD_PROBLEM" -> msg.domainCertificateRecordProblem(domain);
      case "CERTIFICATE_PROBLEM" -> msg.domainCertificateProblem(domain);
      default -> msg.domainHttpsCheckFailed(domain);
    };
  }

  private static String purpose(DnsRecordCheckView record) {
    return record.purpose() == null ? null : record.purpose().toLowerCase(Locale.ROOT);
  }

  private static String detail(DnsRecordCheckView record) {
    String expected = record.expected().isEmpty() ? "-" : String.join(" or ", record.expected());
    return switch (String.valueOf(record.status())) {
      case "MISSING" -> msg.domainRecordMissing(
          purpose(record), record.type(), record.name(), expected);
      case "MISMATCH" -> msg.domainRecordMismatch(
          purpose(record), record.name(), String.join(", ", record.found()), expected);
      default -> msg.domainRecordLookupFailed(purpose(record), record.name());
    };
  }
}
