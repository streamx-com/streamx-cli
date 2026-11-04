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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import picocli.CommandLine;

@CommandLine.Command(
    name = "add",
    header = "Add custom domains to a project and print the DNS records they need",
    description = {
        "Hostnames that the project already has are skipped. Each new hostname gets an ID, its "
            + "key in mesh/cloud-config.yaml: the hostname's first label, numbered when that ID "
            + "is taken, or the one given with --id. For a project with a repository, the "
            + "command commits the change there and waits until it applies."
    }
)
public class AddCommand extends AbstractCommand<List<ProjectDomainView>> {

  @CommandLine.Parameters(
      arity = "1..*",
      paramLabel = "<hostname>",
      description = "Hostname to add, such as shop.example.com"
  )
  public List<String> hostnames;

  @CommandLine.Option(
      names = "--id",
      paramLabel = "<id>",
      description = "ID of the hostname, such as shop; only when adding one hostname"
  )
  public String id;

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @Override
  public String getTextOutput(CommandResult<List<ProjectDomainView>> result) {
    if (result.getData().isEmpty()) {
      return "";
    }
    return DomainsSupport.recordsTable(result.getData().stream()
        .flatMap(domain -> domain.dnsRecords().stream())
        .toList());
  }

  @Override
  public CommandResult<List<ProjectDomainView>> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    List<String> requested = hostnames.stream().map(DomainsSupport::normalize).distinct().toList();
    if (id != null && requested.size() > 1) {
      throw new CliException(msg.domainIdForOneHostname());
    }
    try (PlatformClients client = PlatformClients.fromConfig()) {
      ProjectDomainsApi api = new ProjectDomainsApi(client);
      Map<String, String> desired =
          DomainsSupport.byId(api.list(context.org(), context.project()));
      List<String> added = new ArrayList<>();
      for (String domain : requested) {
        if (desired.containsValue(domain)) {
          System.err.println(msg.domainAlreadyAdded(domain, context.project()));
        } else if (id != null && desired.containsKey(id)) {
          throw new CliException(msg.domainIdTaken(id, desired.get(id)));
        } else {
          desired.put(id != null ? id : DomainsSupport.deriveId(domain, desired.keySet()), domain);
          added.add(domain);
        }
      }
      if (added.isEmpty()) {
        return new CommandResult<>(List.of());
      }

      List<ProjectDomainView> updated = api.update(context.org(), context.project(), desired);
      added.forEach(domain -> System.err.println(msg.domainAdded(domain, context.project())));
      CommandResult<List<ProjectDomainView>> result = new CommandResult<>(updated.stream()
          .filter(domain -> added.contains(domain.domain()))
          .toList());
      // The records are printed either way: they are needed once the change applies.
      if (DomainsSupport.awaitApplied(api, context.org(), context.project(), desired)) {
        System.err.println(msg.domainAddNextStep(added.size() == 1 ? added.get(0) : "<hostname>"));
      } else {
        result.setError(new CliException(msg.domainsChangeNotApplied(context.project())));
      }
      return result;
    }
  }
}
