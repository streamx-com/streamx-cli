package com.streamx.cli.commands.project.domains.list;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.domains.DomainsSupport;
import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.TextTable;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectDomainView;
import com.streamx.cli.platform.ProjectDomainsApi;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    header = "List the custom domains of a project and their verification state"
)
public class ListCommand extends AbstractCommand<List<ProjectDomainView>> {

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @CommandLine.Option(
      names = {"-q", "--quiet"},
      description = "Only display hostnames, one per line (for piping to xargs)"
  )
  public boolean quiet;

  @Override
  public String getTextOutput(CommandResult<List<ProjectDomainView>> result) {
    List<ProjectDomainView> domains = result.getData();

    if (quiet) {
      return domains.stream()
          .map(ProjectDomainView::domain)
          .collect(Collectors.joining("\n"));
    }

    if (domains.isEmpty()) {
      return msg.domainsListEmpty();
    }

    return TextTable.render(
        List.of("ID", "DOMAIN", "STATE", "REASON", "NEXT CHECK"),
        domains.stream()
            .map(domain -> Arrays.asList(
                domain.id(),
                domain.domain(),
                domain.state(),
                DomainsSupport.reasonText(domain.failureReason()),
                // Verified and failed domains are not checked again.
                domain.verified() || domain.failed() ? null : domain.nextCheckAt()))
            .toList());
  }

  @Override
  public CommandResult<List<ProjectDomainView>> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      return new CommandResult<>(
          new ProjectDomainsApi(client).list(context.org(), context.project()));
    }
  }
}
