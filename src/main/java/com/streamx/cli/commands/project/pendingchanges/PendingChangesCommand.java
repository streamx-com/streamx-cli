package com.streamx.cli.commands.project.pendingchanges;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectIdCompletionCandidates;
import com.streamx.cli.platform.ProjectsApi;
import com.streamx.cli.platform.generated.model.PendingChange;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "pending-changes",
    header = "List changes waiting to be applied to a project"
)
public class PendingChangesCommand extends AbstractCommand<List<PendingChange>> {

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

  @Override
  public String getTextOutput(CommandResult<List<PendingChange>> result) {
    List<PendingChange> changes = result.getData();
    if (changes.isEmpty()) {
      return msg.projectPendingChangesEmpty();
    }

    StringBuilder output = new StringBuilder();
    for (PendingChange change : changes) {
      if (!output.isEmpty()) {
        output.append("\n");
      }
      output.append("- ").append(change.getMessage());
      List<String> details = change.getDetails() == null ? List.of() : change.getDetails();
      for (String detail : details) {
        output.append("\n    ").append(detail);
      }
    }
    return output.toString();
  }

  @Override
  public CommandResult<List<PendingChange>> runCommand() {
    PlatformContext.OrgProject context = PlatformContext.orgAndProject(orgId, projectId);
    orgId = context.org();
    projectId = context.project();
    try (PlatformClients client = PlatformClients.fromConfig()) {
      return new CommandResult<>(new ProjectsApi(client).pendingChanges(orgId, projectId));
    }
  }
}
