package com.streamx.cli.commands.project.repo.get;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.DetailsView;
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
    DetailsView details = new DetailsView()
        .row("uri", repository.getUri())
        .row("branch", repository.getBranch())
        .row("commit", repository.getCommitId())
        .row("ready", ready == null ? null : ready.toString())
        .row("ssh key", Boolean.TRUE.equals(repository.getSshKeyProvided())
            ? msg.sshKeySpecified() : msg.sshKeyNotSpecified());
    if (!errors.isEmpty()) {
      details.row("errors", String.join("; ", errors));
    }
    return details.render();
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
