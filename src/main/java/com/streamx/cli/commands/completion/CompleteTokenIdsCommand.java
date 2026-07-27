package com.streamx.cli.commands.completion;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.tokens.ProfileTokensApi;
import com.streamx.cli.platform.tokens.TokenSummary;
import java.util.List;
import java.util.Objects;
import picocli.CommandLine;

@CommandLine.Command(
    name = "__complete-token-ids",
    hidden = true,
    header = "Internal: list personal access token IDs for shell completion, one per line"
)
public class CompleteTokenIdsCommand extends AbstractCommand<List<String>> {

  @Override
  public CommandResult<List<String>> runCommand() {
    try (PlatformClients client = PlatformClients.completion()) {
      return new CommandResult<>(new ProfileTokensApi(client).list().stream()
          .map(TokenSummary::id)
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
