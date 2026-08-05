package com.streamx.cli.commands.auth.token.list;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.TextTable;
import com.streamx.cli.platform.AccessTokens;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.ProfileTokensApi;
import com.streamx.cli.platform.generated.model.PersonalAccessTokenSummary;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    header = "List your personal access tokens"
)
public class ListCommand extends AbstractCommand<List<PersonalAccessTokenSummary>> {

  @CommandLine.Option(
      names = {"-q", "--quiet"},
      description = "Only display token ids, one per line"
  )
  public boolean quiet;

  @Override
  public String getTextOutput(CommandResult<List<PersonalAccessTokenSummary>> result) {
    List<PersonalAccessTokenSummary> tokens = result.getData();

    if (quiet) {
      return tokens.stream()
          .map(PersonalAccessTokenSummary::getId)
          .filter(Objects::nonNull)
          .collect(Collectors.joining("\n"));
    }
    if (tokens.isEmpty()) {
      return msg.authTokenListEmpty();
    }
    return TextTable.render(
        List.of("ID", "NAME", "CREATED", "LAST USED"),
        tokens.stream()
            .map(token -> Arrays.asList(
                token.getId(),
                token.getName(),
                timestamp(token.getCreatedAt(), "-"),
                timestamp(token.getLastUsedAt(), "never")))
            .toList());
  }

  private static String timestamp(OffsetDateTime value, String absent) {
    return value == null ? absent : value.toString();
  }

  @Override
  public CommandResult<List<PersonalAccessTokenSummary>> runCommand() {
    AccessTokens.requireInteractiveSession();
    try (PlatformClients client = PlatformClients.fromConfig()) {
      return new CommandResult<>(new ProfileTokensApi(client).list());
    }
  }
}
