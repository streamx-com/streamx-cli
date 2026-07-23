package com.streamx.cli.commands.project.get;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.Project;
import com.streamx.cli.platform.ProjectsApi;
import picocli.CommandLine;

@CommandLine.Command(
    name = "get",
    header = "Display a project"
)
public class GetCommand extends AbstractCommand<Project> {

  @CommandLine.Parameters(index = "0", description = "Organization ID")
  public String orgId;

  @CommandLine.Parameters(index = "1", description = "Project ID")
  public String projectId;

  @Override
  public String getTextOutput(CommandResult<Project> result) {
    Project project = result.getData();
    return """
        id          = %s
        name        = %s
        description = %s
        state       = %s"""
        .formatted(
            orDash(project.id()),
            orDash(project.name()),
            orDash(project.description()),
            orDash(project.state()));
  }

  private static String orDash(String value) {
    return value == null ? "-" : value;
  }

  @Override
  public CommandResult<Project> runCommand() {
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      return new CommandResult<>(new ProjectsApi(client).get(orgId, projectId));
    }
  }
}
