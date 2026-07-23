package com.streamx.cli.commands.project.update;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.Project;
import com.streamx.cli.platform.ProjectIdCompletionCandidates;
import com.streamx.cli.platform.ProjectsApi;
import picocli.CommandLine;

@CommandLine.Command(
    name = "update",
    header = "Update a project's name or description"
)
public class UpdateCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(
      index = "0",
      arity = "0..1",
      description = "Organization ID (defaults to the current organization)",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @CommandLine.Parameters(
      index = "1",
      arity = "0..1",
      description = "Project ID (defaults to the current project)",
      completionCandidates = ProjectIdCompletionCandidates.class
  )
  public String projectId;

  @CommandLine.Option(names = {"-n", "--name"}, description = "New project name")
  public String name;

  @CommandLine.Option(names = {"-d", "--description"}, description = "New project description")
  public String description;

  @Override
  public CommandResult<Void> runCommand() {
    PlatformContext.OrgProject context = PlatformContext.orgAndProject(orgId, projectId);
    orgId = context.org();
    projectId = context.project();
    if (name == null && description == null) {
      throw new CliException(msg.projectUpdateNothingToDo());
    }

    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      ProjectsApi projects = new ProjectsApi(client);

      // The API replaces both name and description, and name is required, so an unspecified field
      // is filled from the current project to keep this a genuine partial update.
      Project current = projects.get(orgId, projectId);
      String newName = name != null ? name : current.name();
      String newDescription = description != null ? description : current.description();

      projects.update(orgId, projectId, newName, newDescription);
    }
    System.out.println(msg.projectUpdated(projectId));
    return new CommandResult<>(null);
  }
}
