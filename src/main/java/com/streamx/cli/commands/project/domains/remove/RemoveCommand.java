package com.streamx.cli.commands.project.domains.remove;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.domains.DomainsSupport;
import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.DeleteConfirmation;
import com.streamx.cli.platform.DnsRecordView;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectDomainView;
import com.streamx.cli.platform.ProjectDomainsApi;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import picocli.CommandLine;

@CommandLine.Command(
    name = "remove",
    header = "Remove a custom domain from a project",
    description = {
        "Removing a verified domain stops the project from serving it, so the command asks for "
            + "confirmation. For a project with a repository, the command commits the change "
            + "there and waits until it applies."
    }
)
public class RemoveCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "<domain>",
      description = "Domain ID or hostname, as shown by 'streamx project domains list'"
  )
  public String idOrHostname;

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @CommandLine.Option(
      names = {"-f", "--force"},
      description = "Skip the confirmation prompt (required in non-interactive environments)"
  )
  public boolean force;

  @Override
  public CommandResult<Void> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    String requested = DomainsSupport.normalize(idOrHostname);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      ProjectDomainsApi api = new ProjectDomainsApi(client);
      List<ProjectDomainView> current = api.list(context.org(), context.project());
      Optional<ProjectDomainView> target = DomainsSupport.find(current, requested);
      if (target.isEmpty()) {
        System.err.println(msg.domainNotConfigured(requested, context.project()));
        return new CommandResult<>(null);
      }
      String domain = target.get().domain();
      if (target.get().verified()) {
        System.err.println(msg.domainRemoveVerifiedWarning(domain));
        DeleteConfirmation.require(force, domain, msg.domainRemoveConfirmPrompt(domain));
      }

      Map<String, String> desired = DomainsSupport.byId(current.stream()
          .filter(candidate -> !candidate.domain().equals(domain))
          .toList());
      api.update(context.org(), context.project(), desired);
      System.err.println(msg.domainRemoved(domain, context.project()));
      if (!DomainsSupport.awaitApplied(api, context.org(), context.project(), desired)) {
        throw new CliException(msg.domainsChangeNotApplied(context.project()));
      }
      DnsRecordView txt = record(target.get(), "TXT");
      System.err.println(msg.domainRemoveDnsCleanup(
          txt.value(), txt.name(), record(target.get(), "CNAME").name()));
    }
    return new CommandResult<>(null);
  }

  private static DnsRecordView record(ProjectDomainView domain, String type) {
    return domain.dnsRecords().stream()
        .filter(record -> type.equals(record.type()))
        .findFirst()
        .orElse(new DnsRecordView(type, "-", "-"));
  }
}
