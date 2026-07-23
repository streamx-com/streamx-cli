package com.streamx.cli.commands.project.repo.branches;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectRepositoryApi;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "branches",
    header = "List the branches of the connected repository"
)
public class BranchesCommand extends AbstractCommand<List<String>> {

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @Override
  public String getTextOutput(CommandResult<List<String>> result) {
    return String.join("\n", result.getData());
  }

  @Override
  public CommandResult<List<String>> runCommand() {
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      return new CommandResult<>(
          new ProjectRepositoryApi(client).branches(context.org(), context.project()));
    } catch (PlatformApiClient.NotFoundException e) {
      throw new CliException(msg.projectRepoNotConnected(context.project()), e);
    }
  }
}
