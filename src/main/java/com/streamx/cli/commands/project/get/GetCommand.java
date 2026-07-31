package com.streamx.cli.commands.project.get;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectIdCompletionCandidates;
import com.streamx.cli.platform.ProjectView;
import com.streamx.cli.platform.ProjectsApi;
import com.streamx.cli.platform.RepositoryView;
import picocli.CommandLine;

@CommandLine.Command(
    name = "get",
    header = "Display a project, its clusters and its repository"
)
public class GetCommand extends AbstractCommand<ProjectView> {

  @CommandLine.Option(
      names = "--org",
      paramLabel = "<orgId>",
      description = "Organization ID (defaults to the current organization)",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @CommandLine.Parameters(
      index = "0",
      arity = "0..1",
      description = "Project ID (defaults to the current project)",
      completionCandidates = ProjectIdCompletionCandidates.class
  )
  public String projectId;

  @Override
  public String getTextOutput(CommandResult<ProjectView> result) {
    ProjectView project = result.getData();
    RepositoryView repository = project.repository();
    return """
        id          = %s
        name        = %s
        description = %s
        state       = %s
        clusters    = %s
        repository  = %s
        branch      = %s
        ssh key     = %s"""
        .formatted(
            orDash(project.id()),
            orDash(project.name()),
            orDash(project.description()),
            orDash(project.state()),
            project.clusters() == null || project.clusters().isEmpty()
                ? "-" : String.join(", ", project.clusters()),
            repository == null ? msg.repositoryNotConnected() : orDash(repository.uri()),
            repository == null ? "-" : orDash(repository.branch()),
            repository != null && repository.sshKeyProvided()
                ? msg.sshKeySpecified() : msg.sshKeyNotSpecified());
  }

  private static String orDash(String value) {
    return value == null ? "-" : value;
  }

  @Override
  public CommandResult<ProjectView> runCommand() {
    PlatformContext.OrgProject context = PlatformContext.orgAndProject(orgId, projectId);
    orgId = context.org();
    projectId = context.project();
    try (PlatformClients client = PlatformClients.fromConfig()) {
      return new CommandResult<>(new ProjectsApi(client).getDetailed(orgId, projectId));
    }
  }
}
