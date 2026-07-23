package com.streamx.cli.commands.project.delete;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.ProjectsApi;
import picocli.CommandLine;

@CommandLine.Command(
    name = "delete",
    header = "Delete a project"
)
public class DeleteCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(index = "0", description = "Organization ID")
  public String orgId;

  @CommandLine.Parameters(index = "1", description = "Project ID")
  public String projectId;

  @Override
  public CommandResult<Void> runCommand() {
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      new ProjectsApi(client).delete(orgId, projectId);
    }
    System.out.println(msg.projectDeleted(projectId));
    return new CommandResult<>(null);
  }
}
