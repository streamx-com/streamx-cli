package com.streamx.cli.commands.project.repo.remove;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectRepositoryApi;
import picocli.CommandLine;

@CommandLine.Command(
    name = "remove",
    header = "Disconnect the repository from a project",
    description = "Only removes the connection; the Git repository itself is not touched."
)
public class RemoveCommand extends AbstractSilentCommand {

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @Override
  public CommandResult<Void> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      new ProjectRepositoryApi(client).disconnect(context.org(), context.project());
    } catch (PlatformApiClient.NotFoundException e) {
      throw new CliException(msg.projectRepoNotConnected(context.project()), e);
    }
    System.out.println(msg.projectRepoRemoved(context.project()));
    return new CommandResult<>(null);
  }
}
