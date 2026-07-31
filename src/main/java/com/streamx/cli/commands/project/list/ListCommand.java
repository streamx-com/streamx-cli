package com.streamx.cli.commands.project.list;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.TextTable;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectView;
import com.streamx.cli.platform.ProjectsApi;
import com.streamx.cli.platform.RepositoryView;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    header = "List projects in an organization"
)
public class ListCommand extends AbstractCommand<List<ProjectView>> {

  @CommandLine.Option(
      names = "--org",
      paramLabel = "<orgId>",
      description = "Organization ID (defaults to the current organization)",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @CommandLine.Option(
      names = {"-q", "--quiet"},
      description = "Only display project IDs, one per line (for piping to xargs)"
  )
  public boolean quiet;

  @CommandLine.Option(
      names = "--wide",
      description = "Also show each project's clusters and repository "
          + "(one extra request per project)"
  )
  public boolean wide;

  @Override
  public String getTextOutput(CommandResult<List<ProjectView>> result) {
    List<ProjectView> projects = result.getData();

    if (quiet) {
      return projects.stream()
          .map(ProjectView::id)
          .filter(Objects::nonNull)
          .collect(Collectors.joining("\n"));
    }

    if (projects.isEmpty()) {
      return msg.projectListEmpty();
    }

    if (wide) {
      return TextTable.render(
          List.of("ID", "NAME", "STATE", "DESCRIPTION", "CLUSTERS", "REPOSITORY", "SSH KEY"),
          projects.stream()
              .map(project -> Arrays.asList(
                  project.id(), project.name(), project.state(), project.description(),
                  clustersCell(project), repositoryCell(project), sshKeyCell(project)))
              .toList());
    }

    return TextTable.render(
        List.of("ID", "NAME", "STATE", "DESCRIPTION"),
        projects.stream()
            .map(project -> Arrays.asList(
                project.id(), project.name(), project.state(), project.description()))
            .toList());
  }

  private static String clustersCell(ProjectView project) {
    return project.clusters() == null || project.clusters().isEmpty()
        ? "-" : String.join(", ", project.clusters());
  }

  private static String repositoryCell(ProjectView project) {
    RepositoryView repository = project.repository();
    return repository == null || repository.uri() == null ? "-" : repository.uri();
  }

  private static String sshKeyCell(ProjectView project) {
    RepositoryView repository = project.repository();
    if (repository == null) {
      return "-";
    }
    return repository.sshKeyProvided() ? msg.sshKeySpecified() : msg.sshKeyNotSpecified();
  }

  @Override
  public CommandResult<List<ProjectView>> runCommand() {
    orgId = PlatformContext.requireOrg(orgId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      ProjectsApi api = new ProjectsApi(client);
      if (wide) {
        return new CommandResult<>(api.list(orgId).stream()
            .map(project -> api.detailed(orgId, project))
            .toList());
      }
      return new CommandResult<>(api.list(orgId).stream()
          .map(ProjectView::basic)
          .toList());
    }
  }
}
