package com.streamx.cli.commands.project.domains.recheck;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.domains.DomainsSupport;
import com.streamx.cli.commands.project.domains.DurationConverter;
import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.DomainCompletionCandidates;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectDomainView;
import com.streamx.cli.platform.ProjectDomainsApi;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import picocli.CommandLine;

@CommandLine.Command(
    name = "recheck",
    header = "Initiate recheck for the required DNS records",
    description = {
        "Restart pending or failed DNS checks. Checks that already passed are not restarted. "
            + "With --wait, exits with code 1 if the DNS checks fail or waiting times out."
    },
    footer = {
        "",
        "Examples:",
        "  streamx project domains recheck shop.example.com",
        "  streamx project domains recheck shop.example.com --wait --timeout 10m"
    }
)
public class RecheckCommand extends AbstractCommand<ProjectDomainView> {

  private static final Duration POLL_INTERVAL = Duration.ofSeconds(10);

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
      names = "--wait",
      description = "Wait for the DNS checks to finish"
  )
  public boolean wait;

  @CommandLine.Option(
      names = "--timeout",
      paramLabel = "<duration>",
      defaultValue = "1h",
      converter = DurationConverter.class,
      description = "Maximum wait with --wait, such as 10m or 1h (default: ${DEFAULT-VALUE})"
  )
  public Duration timeout;

  @Override
  public String getTextOutput(CommandResult<ProjectDomainView> result) {
    return result.getData() == null ? "" : DomainsSupport.formatDomainDetails(result.getData());
  }

  @Override
  public CommandResult<ProjectDomainView> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      ProjectDomainsApi api = new ProjectDomainsApi(client);
      ProjectDomainView before = DomainsSupport.requireDomain(
          api.list(context.org(), context.project()),
          DomainsSupport.normalizeDomain(idOrDomain),
          context.project());
      String domain = before.domain();
      // The existing background lifecycle does not restart checks that have already passed.
      if (before.dnsChecksPassed()) {
        System.err.println(msg.domainDnsChecksAlreadyPassed(domain));
        return new CommandResult<>(before);
      }
      api.requestDnsCheck(context.org(), context.project(), domain);
      System.err.println(msg.domainDnsCheckRequested(domain));
      if (!wait) {
        System.err.println(msg.domainDnsCheckNextStep(domain));
        return new CommandResult<>(null);
      }
      return awaitResult(api, context, domain, before.lastCheckedAt());
    }
  }

  private CommandResult<ProjectDomainView> awaitResult(
      ProjectDomainsApi api,
      PlatformContext.OrgProject context,
      String domain,
      String checkedBefore
  ) {
    Instant deadline = Instant.now().plus(timeout);
    Duration interval = DomainsSupport.resolvePollInterval(POLL_INTERVAL);
    // The request restarts the DNS check window, so the requested check ends PENDING or
    // VERIFIED. FAILED only counts after that, never from the previous window: a scheduled check
    // may still finish between the read and the request.
    boolean restarted = false;
    String reported = null;
    System.err.println(msg.domainDnsCheckWaiting(domain));
    while (true) {
      ProjectDomainView current = DomainsSupport
          .findDomain(api.list(context.org(), context.project()), domain)
          .orElseThrow(() -> new CliException(
              msg.domainRemovedWhileWaiting(domain, context.project())));
      boolean newCheck = !Objects.equals(current.lastCheckedAt(), checkedBefore);
      if (newCheck && current.dnsChecksPassed()) {
        System.err.println(msg.domainDnsChecksPassed(domain));
        return new CommandResult<>(current);
      }
      restarted = restarted || newCheck && !current.failed();
      if (restarted && current.failed()) {
        return getFailureResult(current, msg.domainDnsChecksFailed(
            domain, DomainsSupport.describeDnsCheckReason(current.failureReason())));
      }
      if (restarted) {
        String progress = getProgress(current);
        if (!progress.equals(reported)) {
          System.err.println(progress);
          reported = progress;
        }
      }
      Duration left = Duration.between(Instant.now(), deadline);
      if (!left.isNegative() && !left.isZero()) {
        DomainsSupport.sleep(left.compareTo(interval) < 0 ? left : interval);
      }
      if (!Instant.now().isBefore(deadline)) {
        return getFailureResult(current,
            msg.domainDnsCheckWaitTimedOut(domain, DurationConverter.format(timeout)));
      }
    }
  }

  private static CommandResult<ProjectDomainView> getFailureResult(
      ProjectDomainView domain,
      String message
  ) {
    CommandResult<ProjectDomainView> result = new CommandResult<>(domain);
    result.setError(new CliException(message));
    return result;
  }

  private static String getProgress(ProjectDomainView domain) {
    String reason = DomainsSupport.describeDnsCheckReason(domain.failureReason());
    if (reason == null) {
      return domain.nextCheckAt() == null
          ? msg.domainCheckingDns(domain.domain())
          : msg.domainCheckingDnsUntil(domain.domain(), domain.nextCheckAt());
    }
    return domain.nextCheckAt() == null
        ? msg.domainDnsChecksPending(reason)
        : msg.domainDnsChecksPendingUntil(reason, domain.nextCheckAt());
  }
}
