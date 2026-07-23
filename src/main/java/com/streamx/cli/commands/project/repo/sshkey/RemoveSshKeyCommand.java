package com.streamx.cli.commands.project.repo.sshkey;

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
    header = "Remove the SSH deploy key from the repository connection"
)
public class RemoveSshKeyCommand extends AbstractSilentCommand {

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @Override
  public CommandResult<Void> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      new ProjectRepositoryApi(client).removeSshKey(context.org(), context.project());
    } catch (PlatformApiClient.NotFoundException e) {
      throw new CliException(msg.projectSshKeyMissing(context.project()), e);
    }
    System.out.println(msg.projectSshKeyRemoved(context.project()));
    return new CommandResult<>(null);
  }
}
