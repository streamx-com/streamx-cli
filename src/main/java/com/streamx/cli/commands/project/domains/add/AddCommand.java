package com.streamx.cli.commands.project.domains.add;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.domains.DomainsSupport;
import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectDomainView;
import com.streamx.cli.platform.ProjectDomainsApi;
import java.util.List;
import java.util.Map;
import picocli.CommandLine;

@CommandLine.Command(
    name = "add",
    header = "Add a custom domain to a project"
)
public class AddCommand extends AbstractCommand<List<ProjectDomainView>> {

  @CommandLine.Parameters(
      index = "0",
      arity = "1",
      paramLabel = "<id>",
      description = "ID of the domain"
  )
  public String id;

  @CommandLine.Parameters(
      index = "1",
      arity = "1",
      paramLabel = "<domain>",
      description = "Domain name to add, such as sub.example.com"
  )
  public String domainName;

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @Override
  public String getTextOutput(CommandResult<List<ProjectDomainView>> result) {
    if (result.getData().isEmpty()) {
      return "";
    }
    return DomainsSupport.formatDnsRecordsTable(result.getData().stream()
        .flatMap(domain -> domain.dnsRecords().stream())
        .toList());
  }

  @Override
  public CommandResult<List<ProjectDomainView>> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    String domain = DomainsSupport.normalizeDomain(domainName);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      ProjectDomainsApi api = new ProjectDomainsApi(client);
      Map<String, String> desired =
          DomainsSupport.domainNamesById(api.list(context.org(), context.project()));
      if (desired.containsValue(domain)) {
        System.err.println(msg.domainAlreadyAdded(domain, context.project()));
        return new CommandResult<>(List.of());
      }
      if (desired.containsKey(id)) {
        throw new CliException(msg.domainIdTaken(id, desired.get(id)));
      }
      desired.put(id, domain);

      List<ProjectDomainView> updated = api.update(context.org(), context.project(), desired);
      System.err.println(msg.domainAdded(domain, context.project()));
      CommandResult<List<ProjectDomainView>> result = new CommandResult<>(updated.stream()
          .filter(updatedDomain -> domain.equals(updatedDomain.domain()))
          .toList());

      // The records are printed either way: they are needed once the change applies.
      if (DomainsSupport.waitForDomainsToApply(api, context.org(), context.project(), desired)) {
        System.err.println(msg.domainAddNextStep(domain));
      } else {
        result.setError(new CliException(msg.domainsChangeNotApplied(context.project())));
      }
      return result;
    }
  }
}
