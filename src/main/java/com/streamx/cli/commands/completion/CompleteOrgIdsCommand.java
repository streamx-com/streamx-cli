package com.streamx.cli.commands.completion;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrganizationsApi;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.generated.model.Organization;
import java.util.List;
import java.util.Objects;
import picocli.CommandLine;

@CommandLine.Command(
    name = "__complete-org-ids",
    hidden = true,
    header = "Internal: list organization IDs for shell completion, one per line"
)
public class CompleteOrgIdsCommand extends AbstractCommand<List<String>> {

  @Override
  public CommandResult<List<String>> runCommand() {
    try (PlatformClients client = PlatformClients.completion()) {
      return new CommandResult<>(new OrganizationsApi(client).list().stream()
          .map(Organization::getId)
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
