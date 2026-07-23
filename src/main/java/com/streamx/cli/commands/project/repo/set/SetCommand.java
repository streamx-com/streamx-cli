package com.streamx.cli.commands.project.repo.set;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectRepositoryApi;
import picocli.CommandLine;

@CommandLine.Command(
    name = "set",
    header = "Connect a repository to a project, or change its settings",
    description = "Connects the repository when the project has none yet, otherwise updates "
        + "the connection. Use 'ssh-key set' first (or 'project create --ssh-private-key') "
        + "for private repositories."
)
public class SetCommand extends AbstractSilentCommand {

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @CommandLine.Option(
      names = "--uri",
      required = true,
      paramLabel = "<uri>",
      description = "Git repository URI"
  )
  public String uri;

  @CommandLine.Option(
      names = "--branch",
      required = true,
      paramLabel = "<branch>",
      description = "Git branch the platform deploys from"
  )
  public String branch;

  @Override
  public CommandResult<Void> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      ProjectRepositoryApi api = new ProjectRepositoryApi(client);
      if (repositoryExists(api, context)) {
        api.update(context.org(), context.project(), uri, branch);
        System.out.println(msg.projectRepoUpdated(context.project()));
      } else {
        api.connect(context.org(), context.project(), uri, branch);
        System.out.println(msg.projectRepoConnected(context.project()));
      }
    }
    return new CommandResult<>(null);
  }

  private static boolean repositoryExists(
      ProjectRepositoryApi api, PlatformContext.OrgProject context) {
    try {
      api.get(context.org(), context.project());
      return true;
    } catch (PlatformApiClient.NotFoundException e) {
      return false;
    }
  }
}
