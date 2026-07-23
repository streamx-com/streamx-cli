package com.streamx.cli.commands.project.status;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.TextTable;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.ProjectStatus;
import com.streamx.cli.platform.ProjectsApi;
import java.util.Arrays;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "status",
    header = "Show a project's deployment status"
)
public class StatusCommand extends AbstractCommand<ProjectStatus> {

  @CommandLine.Parameters(index = "0", description = "Organization ID")
  public String orgId;

  @CommandLine.Parameters(index = "1", description = "Project ID")
  public String projectId;

  @Override
  public String getTextOutput(CommandResult<ProjectStatus> result) {
    ProjectStatus status = result.getData();
    StringBuilder output = new StringBuilder("state = " + orDash(status.state()));

    if (!status.statuses().isEmpty()) {
      output.append("\n\n").append(TextTable.render(
          List.of("STATE", "REASON", "MESSAGE"),
          status.statuses().stream()
              .map(component -> Arrays.asList(
                  component.state(), component.reason(), component.message()))
              .toList()));
    }
    return output.toString();
  }

  private static String orDash(String value) {
    return value == null ? "-" : value;
  }

  @Override
  public CommandResult<ProjectStatus> runCommand() {
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      return new CommandResult<>(new ProjectsApi(client).status(orgId, projectId));
    }
  }
}
