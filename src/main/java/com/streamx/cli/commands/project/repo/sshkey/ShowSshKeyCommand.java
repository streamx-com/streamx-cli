package com.streamx.cli.commands.project.repo.sshkey;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectRepositoryApi;
import picocli.CommandLine;

@CommandLine.Command(
    name = "show",
    header = "Print the public key of the configured SSH deploy key",
    description = "Add this public key to the Git hosting's deploy keys. "
        + "The private key never leaves the platform."
)
public class ShowSshKeyCommand extends AbstractCommand<String> {

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @Override
  public String getTextOutput(CommandResult<String> result) {
    return result.getData();
  }

  @Override
  public CommandResult<String> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      String publicKey = new ProjectRepositoryApi(client)
          .publicKey(context.org(), context.project());
      if (publicKey == null) {
        throw new CliException(msg.projectSshKeyMissing(context.project()));
      }
      return new CommandResult<>(publicKey);
    } catch (PlatformApiClient.NotFoundException e) {
      throw new CliException(msg.projectSshKeyMissing(context.project()), e);
    }
  }
}
