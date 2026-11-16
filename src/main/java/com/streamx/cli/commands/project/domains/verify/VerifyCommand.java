package com.streamx.cli.commands.project.domains.verify;

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
    name = "verify",
    header = "Request domain verification",
    description = {
        "Request a DNS verification check. Use --wait to wait for the result. "
            + "With --wait, exits with code 1 if verification fails or waiting times out."
    },
    footer = {
        "",
        "Examples:",
        "  streamx project domains verify shop.example.com",
        "  streamx project domains verify shop.example.com --wait --timeout 10m"
    }
)
public class VerifyCommand extends AbstractCommand<ProjectDomainView> {

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
      description = "Wait for verification to succeed or fail"
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
      // The platform ignores requests for verified domains, though it answers PENDING to them.
      if (before.verified()) {
        System.err.println(msg.domainAlreadyVerified(domain));
        return new CommandResult<>(before);
      }
      api.verify(context.org(), context.project(), domain);
      System.err.println(msg.domainVerificationRequested(domain));
      if (!wait) {
        System.err.println(msg.domainVerificationNextStep(domain));
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
    // The request restarts the verification window, so the requested check ends PENDING or
    // VERIFIED. FAILED only counts after that, never from the previous window: a scheduled check
    // may still finish between the read and the request.
    boolean restarted = false;
    String reported = null;
    System.err.println(msg.domainVerificationWaiting(domain));
    while (true) {
      ProjectDomainView current = DomainsSupport
          .findDomain(api.list(context.org(), context.project()), domain)
          .orElseThrow(() -> new CliException(
              msg.domainRemovedWhileWaiting(domain, context.project())));
      boolean newCheck = !Objects.equals(current.lastCheckedAt(), checkedBefore);
      if (newCheck && current.verified()) {
        System.err.println(msg.domainVerified(domain));
        return new CommandResult<>(current);
      }
      restarted = restarted || newCheck && !current.failed();
      if (restarted && current.failed()) {
        return getFailureResult(current, msg.domainVerificationFailed(
            domain, DomainsSupport.describeVerificationReason(current.failureReason())));
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
            msg.domainVerificationTimedOut(domain, DurationConverter.format(timeout)));
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
    String reason = DomainsSupport.describeVerificationReason(domain.failureReason());
    if (reason == null) {
      return domain.nextCheckAt() == null
          ? msg.domainVerifying(domain.domain())
          : msg.domainVerifyingUntil(domain.domain(), domain.nextCheckAt());
    }
    return domain.nextCheckAt() == null
        ? msg.domainVerificationPending(reason)
        : msg.domainVerificationPendingUntil(reason, domain.nextCheckAt());
  }
}
