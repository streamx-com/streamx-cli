package com.streamx.cli.commands.project.domains.remove;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.domains.DomainsSupport;
import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.DeleteConfirmation;
import com.streamx.cli.platform.DnsRecordView;
import com.streamx.cli.platform.DomainCompletionCandidates;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectDomainView;
import com.streamx.cli.platform.ProjectDomainsApi;
import com.streamx.cli.platform.generated.model.RecheckDomainDns200ResponseDnsRecordsInner.TypeEnum;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import picocli.CommandLine;

@CommandLine.Command(
    name = "remove",
    header = "Remove a custom domain from a project",
    description = "Remove a domain from the project and wait for the change to apply."
)
public class RemoveCommand extends AbstractSilentCommand {

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
      names = {"-f", "--force"},
      description = "Remove without asking for confirmation"
  )
  public boolean force;

  @Override
  public CommandResult<Void> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    String requested = DomainsSupport.normalizeDomain(idOrDomain);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      ProjectDomainsApi api = new ProjectDomainsApi(client);
      List<ProjectDomainView> current = api.list(context.org(), context.project());
      Optional<ProjectDomainView> target = DomainsSupport.findDomain(current, requested);
      if (target.isEmpty()) {
        System.err.println(msg.domainNotConfigured(requested, context.project()));
        return new CommandResult<>(null);
      }
      String domain = target.get().domain();
      if (target.get().dnsChecksPassed()) {
        System.err.println(msg.domainRemoveWarning(domain));
        DeleteConfirmation.require(force, domain, msg.domainRemoveConfirmPrompt(domain));
      }

      Map<String, String> desired = DomainsSupport.domainNamesById(current.stream()
          .filter(candidate -> !candidate.domain().equals(domain))
          .toList());
      api.update(context.org(), context.project(), desired);
      System.err.println(msg.domainRemoved(domain, context.project()));
      if (!DomainsSupport.waitForDomainsToApply(api, context.org(), context.project(), desired)) {
        throw new CliException(msg.domainsChangeNotApplied(context.project()));
      }
      DnsRecordView txt = record(target.get(), TypeEnum.TXT);
      System.err.println(msg.domainRemoveDnsCleanup(
          txt.value(), txt.name(), record(target.get(), TypeEnum.CNAME).name()));
    }
    return new CommandResult<>(null);
  }

  private static DnsRecordView record(ProjectDomainView domain, TypeEnum type) {
    return domain.dnsRecords().stream()
        .filter(record -> type == record.type())
        .findFirst()
        .orElse(new DnsRecordView(type, "-", "-"));
  }
}
