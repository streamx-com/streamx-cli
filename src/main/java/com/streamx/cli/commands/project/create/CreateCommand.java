package com.streamx.cli.commands.project.create;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.Project;
import com.streamx.cli.platform.ProjectsApi;
import picocli.CommandLine;

@CommandLine.Command(
    name = "create",
    header = "Create a project"
)
public class CreateCommand extends AbstractCommand<Project> {

  @CommandLine.Parameters(
      index = "0",
      description = "Organization ID",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @CommandLine.Parameters(index = "1", description = "Project name")
  public String name;

  @CommandLine.Option(
      names = {"-d", "--description"},
      description = "Project description"
  )
  public String description;

  @Override
  public String getTextOutput(CommandResult<Project> result) {
    Project project = result.getData();
    return msg.projectCreated(name, project.id());
  }

  @Override
  public CommandResult<Project> runCommand() {
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      return new CommandResult<>(new ProjectsApi(client).create(orgId, name, description));
    }
  }
}
