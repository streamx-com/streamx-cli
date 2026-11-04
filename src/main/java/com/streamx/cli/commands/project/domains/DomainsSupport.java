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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** What the {@code project domains} subcommands share: lookups, waiting and rendering. */
public final class DomainsSupport {

  // Shorter waits for tests, read like STREAMX_PLATFORM_TOKEN: environment, then system property.
  public static final String POLL_INTERVAL_MS = "STREAMX_DOMAINS_POLL_INTERVAL_MS";
  // A project with a repository applies a change once its repository deployment syncs.
  private static final Duration APPLY_POLL_INTERVAL = Duration.ofSeconds(3);
  private static final int APPLY_POLLS = 40;
  // Domain IDs, the keys in mesh/cloud-config.yaml, are lowercase DNS labels.
  private static final Pattern DOMAIN_ID = Pattern.compile("[a-z0-9]([-a-z0-9]*[a-z0-9])?");
  private static final int MAX_DOMAIN_ID_LENGTH = 63;
  private static final String FALLBACK_DOMAIN_ID = "domain";

  private DomainsSupport() {
  }

  public static String normalize(String hostname) {
    return hostname.trim().toLowerCase(Locale.ROOT);
  }

  /**
   * The ID a new domain gets unless one is given, derived as the dashboard and the platform do:
   * the hostname's first label, with {@code -2}, {@code -3}... when that ID is taken.
   */
  public static String deriveId(String hostname, Collection<String> taken) {
    String normalized = normalize(hostname);
    int dot = normalized.indexOf('.');
    String label = dot < 0 ? normalized : normalized.substring(0, dot);
    String base = label.length() <= MAX_DOMAIN_ID_LENGTH && DOMAIN_ID.matcher(label).matches()
        ? label : FALLBACK_DOMAIN_ID;
    String id = base;
    for (int number = 2; taken.contains(id); number++) {
      String suffix = "-" + number;
      id = base.substring(0, Math.min(base.length(), MAX_DOMAIN_ID_LENGTH - suffix.length()))
          + suffix;
    }
    return id;
  }

  /** The domains as the API takes them: hostnames by ID, in the listed order. */
  public static Map<String, String> byId(List<ProjectDomainView> domains) {
    Map<String, String> byId = new LinkedHashMap<>();
    domains.forEach(domain -> byId.put(domain.id(), domain.domain()));
    return byId;
  }

  /**
   * The domain that {@code domain}, already normalized, names by its ID or its hostname. They
   * cannot be confused: an ID never contains a dot, and a valid hostname always does.
   */
  public static Optional<ProjectDomainView> find(List<ProjectDomainView> domains, String domain) {
    return domains.stream()
        .filter(candidate -> domain.equals(candidate.id()) || domain.equals(candidate.domain()))
        .findFirst();
  }

  public static ProjectDomainView require(List<ProjectDomainView> domains, String domain,
      String projectId) {
    return find(domains, domain)
        .orElseThrow(() -> new CliException(msg.domainNotConfigured(domain, projectId)));
  }

  /**
   * Polls until the project's applied domains, hostnames by ID, are {@code expected}; false if
   * they never are.
   */
  public static boolean awaitApplied(ProjectDomainsApi api, String orgId, String projectId,
      Map<String, String> expected) {
    Duration interval = pollInterval(APPLY_POLL_INTERVAL);
    for (int poll = 0; poll <= APPLY_POLLS; poll++) {
      if (poll > 0) {
        sleep(interval);
      }
      if (byId(api.list(orgId, projectId)).equals(expected)) {
        return true;
      }
    }
    return false;
  }

  public static Duration pollInterval(Duration standard) {
    String value = System.getenv(POLL_INTERVAL_MS);
    if (value == null || value.isBlank()) {
      value = System.getProperty(POLL_INTERVAL_MS);
    }
    return value == null || value.isBlank()
        ? standard : Duration.ofMillis(Long.parseLong(value.trim()));
  }

  public static void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CliException(msg.domainsWaitInterrupted(), e);
    }
  }

  public static String reasonText(String reason) {
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

  public static String details(ProjectDomainView domain) {
    String details = new DetailsView()
        .row("id", domain.id())
        .row("domain", domain.domain())
        .row("state", domain.state())
        .row("reason", reasonText(domain.failureReason()))
        .row("attempts", domain.attempts() == null ? null : domain.attempts().toString())
        .row("last check", domain.lastCheckedAt())
        .row("next check", domain.nextCheckAt())
        .row("verified at", domain.verifiedAt())
        .render();
    return details + System.lineSeparator() + System.lineSeparator()
        + recordsTable(domain.dnsRecords());
  }

  public static String recordsTable(List<DnsRecordView> records) {
    return TextTable.render(
        List.of("TYPE", "NAME", "VALUE"),
        records.stream()
            .map(record -> Arrays.asList(record.type(), record.name(), record.value()))
            .toList());
  }

  /** Zone-file lines with absolute names; the DNS provider's default TTL applies. */
  public static String zoneLines(List<DnsRecordView> records) {
    return records.stream()
        .map(DomainsSupport::zoneLine)
        .collect(Collectors.joining(System.lineSeparator()));
  }

  static String zoneLine(DnsRecordView record) {
    String value = "TXT".equals(record.type())
        ? "\"" + record.value().replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        : absolute(record.value());
    return absolute(record.name()) + " IN " + record.type() + " " + value;
  }

  private static String absolute(String name) {
    return name.endsWith(".") ? name : name + ".";
  }
}
