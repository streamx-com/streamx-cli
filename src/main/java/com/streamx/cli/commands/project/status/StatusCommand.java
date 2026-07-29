package com.streamx.cli.commands.project.status;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.TextTable;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectIdCompletionCandidates;
import com.streamx.cli.platform.ProjectsApi;
import com.streamx.cli.platform.generated.model.ProjectStatus;
import java.util.Arrays;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "status",
    header = "Show a project's deployment status"
)
public class StatusCommand extends AbstractCommand<ProjectStatus> {

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
  public String getTextOutput(CommandResult<ProjectStatus> result) {
    ProjectStatus status = result.getData();
    StringBuilder output = new StringBuilder("state = "
        + orDash(status.getState() == null ? null : status.getState().value()));

    if (status.getStatuses() != null && !status.getStatuses().isEmpty()) {
      output.append("\n\n").append(TextTable.render(
          List.of("STATE", "REASON", "MESSAGE"),
          status.getStatuses().stream()
              .map(component -> Arrays.asList(
                  component.getState() == null ? null : component.getState().value(),
                  component.getReason(), component.getMessage()))
              .toList()));
    }
    return output.toString();
  }

  private static String orDash(String value) {
    return value == null ? "-" : value;
  }

  @Override
  public CommandResult<ProjectStatus> runCommand() {
    PlatformContext.OrgProject context = PlatformContext.orgAndProject(orgId, projectId);
    orgId = context.org();
    projectId = context.project();
    try (PlatformClients client = PlatformClients.fromConfig()) {
      return new CommandResult<>(new ProjectsApi(client).status(orgId, projectId));
    }
  }
}
