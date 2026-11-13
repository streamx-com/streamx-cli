package com.streamx.cli.commands.project.domains;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.DetailsView;
import com.streamx.cli.framework.TextTable;
import com.streamx.cli.platform.DnsRecordView;
import com.streamx.cli.platform.ProjectDomainView;
import com.streamx.cli.platform.ProjectDomainsApi;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** What the {@code project domains} subcommands share: lookups, waiting and rendering. */
public final class DomainsSupport {

  // Shorter waits for tests.
  public static final String POLL_INTERVAL_MS = "STREAMX_DOMAINS_POLL_INTERVAL_MS";
  // A project with a repository applies a change once its repository deployment syncs.
  private static final Duration APPLY_POLL_INTERVAL = Duration.ofSeconds(3);
  private static final int APPLY_POLLS = 40;

  private DomainsSupport() {
  }

  public static String normalizeDomain(String domain) {
    return domain.trim().toLowerCase(Locale.ROOT);
  }

  /** The domains as the API takes them: domain names by ID, in the listed order. */
  public static Map<String, String> domainNamesById(List<ProjectDomainView> domains) {
    Map<String, String> byId = new LinkedHashMap<>();
    domains.forEach(domain -> byId.put(domain.id(), domain.domain()));
    return byId;
  }

  /** Finds a domain by its normalized ID or domain name. */
  public static Optional<ProjectDomainView> findDomain(List<ProjectDomainView> domains,
      String domain) {
    return domains.stream()
        .filter(candidate -> domain.equals(candidate.id()) || domain.equals(candidate.domain()))
        .findFirst();
  }

  public static ProjectDomainView requireDomain(List<ProjectDomainView> domains, String domain,
      String projectId) {
    return findDomain(domains, domain)
        .orElseThrow(() -> new CliException(msg.domainNotConfigured(domain, projectId)));
  }

  /**
   * Waits until the API returns the expected domain names and IDs. Returns false if the
   * configured polling limit is reached first.
   */
  public static boolean waitForDomainsToApply(ProjectDomainsApi api, String orgId, String projectId,
      Map<String, String> expected) {
    Duration interval = resolvePollInterval(APPLY_POLL_INTERVAL);
    for (int poll = 0; poll <= APPLY_POLLS; poll++) {
      if (poll > 0) {
        sleep(interval);
      }
      if (domainNamesById(api.list(orgId, projectId)).equals(expected)) {
        return true;
      }
    }
    return false;
  }

  public static Duration resolvePollInterval(Duration standard) {
    String interval = System.getenv(POLL_INTERVAL_MS);
    if (interval == null || interval.isBlank()) {
      interval = System.getProperty(POLL_INTERVAL_MS);
    }
    return interval == null || interval.isBlank()
        ? standard : Duration.ofMillis(Long.parseLong(interval.trim()));
  }

  public static void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CliException(msg.domainsWaitInterrupted(), e);
    }
  }

  public static String describeVerificationReason(String reason) {
    if (reason == null) {
      return null;
    }
    return switch (reason) {
      case "TXT_RECORD_NOT_FOUND" -> msg.domainReasonTxtRecordNotFound();
      case "CNAME_RECORD_NOT_FOUND" -> msg.domainReasonCnameRecordNotFound();
      case "CNAME_TARGET_MISMATCH" -> msg.domainReasonCnameTargetMismatch();
      case "DNS_LOOKUP_FAILED" -> msg.domainReasonDnsLookupFailed();
      case "VERIFICATION_WINDOW_EXPIRED" -> msg.domainReasonVerificationWindowExpired();
      case "ORGANIZATION_NOT_RESOLVED" -> msg.domainReasonOrganizationNotResolved();
      default -> reason;
    };
  }

  public static String formatDomainDetails(ProjectDomainView domain) {
    String details = new DetailsView()
        .row("id", domain.id())
        .row("domain", domain.domain())
        .row("state", domain.state())
        .row("reason", describeVerificationReason(domain.failureReason()))
        .row("attempts", domain.attempts() == null ? null : domain.attempts().toString())
        .row("last check", domain.lastCheckedAt())
        .row("next check", domain.nextCheckAt())
        .row("verified at", domain.verifiedAt())
        .render();
    return details + System.lineSeparator() + System.lineSeparator()
        + formatDnsRecordsTable(domain.dnsRecords());
  }

  public static String formatDnsRecordsTable(List<DnsRecordView> records) {
    return TextTable.render(
        List.of("TYPE", "NAME", "VALUE"),
        records.stream()
            .map(record -> Arrays.asList(record.type(), record.name(), record.value()))
            .toList());
  }

  /** Zone-file lines with absolute names; the DNS provider's default TTL applies. */
  public static String formatDnsZoneFile(List<DnsRecordView> records) {
    return records.stream()
        .map(DomainsSupport::formatDnsZoneRecord)
        .collect(Collectors.joining(System.lineSeparator()));
  }

  static String formatDnsZoneRecord(DnsRecordView record) {
    String value = "TXT".equals(record.type())
        ? "\"" + record.value().replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        : toAbsoluteDnsName(record.value());
    return toAbsoluteDnsName(record.name()) + " IN " + record.type() + " " + value;
  }

  private static String toAbsoluteDnsName(String name) {
    return name.endsWith(".") ? name : name + ".";
  }
}
