package com.streamx.cli.commands.completion;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.Project;
import com.streamx.cli.platform.ProjectsApi;
import java.util.List;
import java.util.Objects;
import picocli.CommandLine;

@CommandLine.Command(
    name = "__complete-project-ids",
    hidden = true,
    header = "Internal: list project IDs of an organization for shell completion"
)
public class CompleteProjectIdsCommand extends AbstractCommand<List<String>> {

  @CommandLine.Parameters(index = "0", arity = "0..1", description = "Organization ID")
  public String orgId;

  @Override
  public CommandResult<List<String>> runCommand() {
    // Shell completion must stay silent and fast on ANY failure (no org typed yet, not
    // logged in, platform unreachable) - an empty list simply completes nothing.
    if (orgId == null || orgId.isBlank() || orgId.startsWith("-")) {
      return new CommandResult<>(List.of());
    }
    try (PlatformApiClient client = PlatformApiClient.completionClient()) {
      return new CommandResult<>(new ProjectsApi(client).list(orgId).stream()
          .map(Project::id)
          .filter(Objects::nonNull)
          .toList());
    } catch (RuntimeException anyFailure) {
      return new CommandResult<>(List.of());
    }
  }

  @Override
  public String getTextOutput(CommandResult<List<String>> result) {
    return String.join("\n", result.getData());
  }
}
