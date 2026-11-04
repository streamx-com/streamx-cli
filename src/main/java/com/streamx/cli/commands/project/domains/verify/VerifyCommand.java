package com.streamx.cli.commands.project.domains.verify;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.domains.DomainsSupport;
import com.streamx.cli.commands.project.domains.DurationConverter;
import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
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
    header = "Check a custom domain's DNS records again",
    description = {
        "Asks the platform to check the domain's DNS records now, which also restarts its "
            + "verification window. With --wait, the command follows the checks and ends with "
            + "exit code 0 once the domain is verified, or 1 if it fails or the time runs out."
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
      description = "Domain ID or hostname, as shown by 'streamx project domains list'"
  )
  public String idOrHostname;

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @CommandLine.Option(
      names = "--wait",
      description = "Wait until the domain is verified or fails"
  )
  public boolean wait;

  @CommandLine.Option(
      names = "--timeout",
      paramLabel = "<duration>",
      defaultValue = "1h",
      converter = DurationConverter.class,
      description = "How long --wait waits, such as 90s, 10m or 1h (default: ${DEFAULT-VALUE})"
  )
  public Duration timeout;

  @Override
  public String getTextOutput(CommandResult<ProjectDomainView> result) {
    return result.getData() == null ? "" : DomainsSupport.details(result.getData());
  }

  @Override
  public CommandResult<ProjectDomainView> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      ProjectDomainsApi api = new ProjectDomainsApi(client);
      ProjectDomainView before = DomainsSupport.require(
          api.list(context.org(), context.project()), DomainsSupport.normalize(idOrHostname),
          context.project());
      String domain = before.domain();
      // The platform ignores requests for verified domains, though it answers PENDING to them.
      if (before.verified()) {
        System.err.println(msg.domainAlreadyVerified(domain));
        System.err.println(msg.domainVerifiedNextStep(domain));
        return new CommandResult<>(before);
      }
      api.verify(context.org(), context.project(), domain);
      System.err.println(msg.domainVerificationRequested(domain));
      if (!wait) {
        System.err.println(msg.domainVerificationNextStep(domain));
        return new CommandResult<>(null);
      }
      return await(api, context, domain, before.lastCheckedAt());
    }
  }

  private CommandResult<ProjectDomainView> await(ProjectDomainsApi api,
      PlatformContext.OrgProject context, String domain, String checkedBefore) {
    Instant deadline = Instant.now().plus(timeout);
    Duration interval = DomainsSupport.pollInterval(POLL_INTERVAL);
    // The request restarts the verification window, so the requested check ends PENDING or
    // VERIFIED. FAILED only counts after that, never from the previous window: a scheduled check
    // may still finish between the read and the request.
    boolean restarted = false;
    String reported = null;
    System.err.println(msg.domainVerificationWaiting(domain));
    while (true) {
      ProjectDomainView current = DomainsSupport
          .find(api.list(context.org(), context.project()), domain)
          .orElseThrow(() -> new CliException(
              msg.domainRemovedWhileWaiting(domain, context.project())));
      boolean newCheck = !Objects.equals(current.lastCheckedAt(), checkedBefore);
      if (newCheck && current.verified()) {
        System.err.println(msg.domainVerified(domain));
        System.err.println(msg.domainVerifiedNextStep(domain));
        return new CommandResult<>(current);
      }
      restarted = restarted || newCheck && !current.failed();
      if (restarted && current.failed()) {
        return failure(current, msg.domainVerificationFailed(
            domain, DomainsSupport.reasonText(current.failureReason())));
      }
      if (restarted) {
        String progress = progress(current);
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
        return failure(current,
            msg.domainVerificationTimedOut(domain, DurationConverter.format(timeout)));
      }
    }
  }

  private static CommandResult<ProjectDomainView> failure(ProjectDomainView domain,
      String message) {
    CommandResult<ProjectDomainView> result = new CommandResult<>(domain);
    result.setError(new CliException(message));
    return result;
  }

  private static String progress(ProjectDomainView domain) {
    String reason = Objects.requireNonNullElse(
        DomainsSupport.reasonText(domain.failureReason()), domain.state());
    return domain.nextCheckAt() == null
        ? msg.domainVerificationPending(reason)
        : msg.domainVerificationPendingUntil(reason, domain.nextCheckAt());
  }
}
