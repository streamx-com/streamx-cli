package com.streamx.cli.commands.completion;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectDomainsApi;
import java.util.List;
import java.util.stream.Stream;
import picocli.CommandLine;

@CommandLine.Command(
    name = "__complete-domains",
    hidden = true,
    header = "Internal: list domain IDs and names for shell completion"
)
public class CompleteDomainsCommand extends AbstractCommand<List<String>> {

  @CommandLine.Parameters(index = "0", arity = "0..1", description = "Organization ID")
  public String orgId;

  @CommandLine.Parameters(index = "1", arity = "0..1", description = "Project ID")
  public String projectId;

  @Override
  public CommandResult<List<String>> runCommand() {
    try {
      PlatformContext.OrgProject context = PlatformContext.orgAndProject(orgId,
          projectId == null || projectId.isBlank() ? null : projectId);
      try (PlatformClients client = PlatformClients.completion()) {
        return new CommandResult<>(new ProjectDomainsApi(client)
            .list(context.org(), context.project()).stream()
            .flatMap(domain -> Stream.of(domain.id(), domain.domain()))
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .sorted()
            .toList());
      }
    } catch (RuntimeException anyFailure) {
      return new CommandResult<>(List.of());
    }
  }

  @Override
  public String getTextOutput(CommandResult<List<String>> result) {
    return String.join("\n", result.getData());
  }
}
