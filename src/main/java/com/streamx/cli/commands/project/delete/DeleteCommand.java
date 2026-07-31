package com.streamx.cli.commands.project.delete;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.DeleteConfirmation;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
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

  @CommandLine.Option(
      names = "--org",
      paramLabel = "<orgId>",
      description = "Organization ID (defaults to the current organization)",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @CommandLine.Parameters(
      index = "0",
      arity = "0..1",
      description = "Project ID (defaults to the current project)",
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
    PlatformContext.OrgProject context = PlatformContext.orgAndProject(orgId, projectId);
    orgId = context.org();
    projectId = context.project();
    DeleteConfirmation.require(force, projectId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      new ProjectsApi(client).delete(orgId, projectId);
    }
    System.out.println(msg.projectDeleted(projectId));
    return new CommandResult<>(null);
  }
}
