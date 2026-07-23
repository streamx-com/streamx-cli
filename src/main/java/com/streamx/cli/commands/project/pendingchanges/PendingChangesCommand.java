package com.streamx.cli.commands.project.pendingchanges;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.PendingChange;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.ProjectIdCompletionCandidates;
import com.streamx.cli.platform.ProjectsApi;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "pending-changes",
    header = "List changes waiting to be applied to a project"
)
public class PendingChangesCommand extends AbstractCommand<List<PendingChange>> {

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
      output.append("- ").append(change.message());
      for (String detail : change.details()) {
        output.append("\n    ").append(detail);
      }
    }
    return output.toString();
  }

  @Override
  public CommandResult<List<PendingChange>> runCommand() {
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      return new CommandResult<>(new ProjectsApi(client).pendingChanges(orgId, projectId));
    }
  }
}
