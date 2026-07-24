package com.streamx.cli.commands.project.repo.get;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectRepository;
import com.streamx.cli.platform.ProjectRepositoryApi;
import picocli.CommandLine;

@CommandLine.Command(
    name = "get",
    header = "Show the repository connected to a project"
)
public class GetCommand extends AbstractCommand<ProjectRepository> {

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @Override
  public String getTextOutput(CommandResult<ProjectRepository> result) {
    ProjectRepository repository = result.getData();
    return """
        uri       = %s
        branch    = %s
        commit    = %s
        ready     = %s
        ssh key   = %s%s"""
        .formatted(
            orDash(repository.uri()),
            orDash(repository.branch()),
            orDash(repository.commitId()),
            repository.ready() == null ? "-" : repository.ready(),
            repository.sshKeyProvided() ? msg.sshKeySpecified() : msg.sshKeyNotSpecified(),
            repository.errorMessages().isEmpty() ? ""
                : "\nerrors    = " + String.join("; ", repository.errorMessages()));
  }

  private static String orDash(String value) {
    return value == null ? "-" : value;
  }

  @Override
  public CommandResult<ProjectRepository> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      return new CommandResult<>(new ProjectRepositoryApi(client)
          .get(context.org(), context.project()));
    } catch (PlatformApiClient.NotFoundException e) {
      throw new CliException(msg.projectRepoNotConnected(context.project()), e);
    }
  }
}
