package com.streamx.cli.commands.project.list;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.TextTable;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.Project;
import com.streamx.cli.platform.ProjectsApi;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    header = "List projects in an organization"
)
public class ListCommand extends AbstractCommand<List<Project>> {

  @CommandLine.Parameters(
      index = "0",
      arity = "0..1",
      description = "Organization ID (defaults to the current organization)",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @CommandLine.Option(
      names = {"-q", "--quiet"},
      description = "Only display project IDs, one per line (for piping to xargs)"
  )
  public boolean quiet;

  @Override
  public String getTextOutput(CommandResult<List<Project>> result) {
    List<Project> projects = result.getData();

    if (quiet) {
      return projects.stream()
          .map(Project::id)
          .filter(Objects::nonNull)
          .collect(Collectors.joining("\n"));
    }

    if (projects.isEmpty()) {
      return msg.projectListEmpty();
    }

    return TextTable.render(
        List.of("ID", "NAME", "STATE", "DESCRIPTION"),
        projects.stream()
            .map(project -> Arrays.asList(
                project.id(), project.name(), project.state(), project.description()))
            .toList());
  }

  @Override
  public CommandResult<List<Project>> runCommand() {
    orgId = PlatformContext.requireOrg(orgId);
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      return new CommandResult<>(new ProjectsApi(client).list(orgId));
    }
  }
}
