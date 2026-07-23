package com.streamx.cli.commands.project.delete;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.DeleteConfirmation;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.ProjectIdCompletionCandidates;
import com.streamx.cli.platform.ProjectsApi;
import picocli.CommandLine;

@CommandLine.Command(
    name = "delete",
    header = "Delete a project",
    description = "Asks to type the project ID back as confirmation; "
        + "--force deletes without asking."
)
public class DeleteCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(
      index = "0",
      description = "Organization ID",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @CommandLine.Parameters(
      index = "1",
      description = "Project ID",
      completionCandidates = ProjectIdCompletionCandidates.class
  )
  public String projectId;

  @CommandLine.Option(
      names = {"-f", "--force"},
      description = "Skip the confirmation prompt (required in non-interactive environments)"
  )
  public boolean force;

  @Override
  public CommandResult<Void> runCommand() {
    DeleteConfirmation.require(force, projectId);
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      new ProjectsApi(client).delete(orgId, projectId);
    }
    System.out.println(msg.projectDeleted(projectId));
    return new CommandResult<>(null);
  }
}
