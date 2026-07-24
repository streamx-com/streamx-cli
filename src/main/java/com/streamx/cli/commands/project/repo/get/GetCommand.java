package com.streamx.cli.commands.project.repo.get;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectRepositoryApi;
import com.streamx.cli.platform.generated.model.ProjectRepository;
import java.util.List;
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
    var status = repository.getProjectRepositoryStatus();
    Boolean ready = status == null ? null : status.getReady();
    List<String> errors = status == null || status.getErrorMessages() == null
        ? List.of() : status.getErrorMessages();
    return """
        uri       = %s
        branch    = %s
        commit    = %s
        ready     = %s
        ssh key   = %s%s"""
        .formatted(
            orDash(repository.getUri()),
            orDash(repository.getBranch()),
            orDash(repository.getCommitId()),
            ready == null ? "-" : ready,
            Boolean.TRUE.equals(repository.getSshKeyProvided())
                ? msg.sshKeySpecified() : msg.sshKeyNotSpecified(),
            errors.isEmpty() ? "" : "\nerrors    = " + String.join("; ", errors));
  }

  private static String orDash(String value) {
    return value == null ? "-" : value;
  }

  @Override
  public CommandResult<ProjectRepository> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      return new CommandResult<>(new ProjectRepositoryApi(client)
          .get(context.org(), context.project()));
    } catch (PlatformClients.NotFoundException e) {
      throw new CliException(msg.projectRepoNotConnected(context.project()), e);
    }
  }
}
