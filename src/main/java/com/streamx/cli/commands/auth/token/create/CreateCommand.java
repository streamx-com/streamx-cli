package com.streamx.cli.commands.auth.token.create;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.AccessTokens;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.ProfileTokensApi;
import com.streamx.cli.platform.generated.model.PersonalAccessTokenResponse;
import io.quarkus.runtime.configuration.DurationConverter;
import java.time.Duration;
import picocli.CommandLine;

@CommandLine.Command(
    name = "create",
    header = "Create a personal access token",
    description = {
        "Prints the token once to standard output - copy it now, it cannot be retrieved again."
    }
)
public class CreateCommand extends AbstractCommand<PersonalAccessTokenResponse> {

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "<name>",
      description = "A label to recognize the token later (e.g. ci-github-actions)"
  )
  public String name;

  @CommandLine.Option(
      names = {"-e", "--expires-in"},
      paramLabel = "<duration>",
      description = {
          "How long the token stays valid, e.g. 20m, 2h, 30d.",
          "Omit for a token that never expires."
      }
  )
  public String expiresIn;

  @Override
  public String getTextOutput(CommandResult<PersonalAccessTokenResponse> result) {
    return result.getData().getToken();
  }

  @Override
  public CommandResult<PersonalAccessTokenResponse> runCommand() {
    AccessTokens.requireInteractiveSession();
    Duration lifetime = parseLifetime(expiresIn);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      PersonalAccessTokenResponse token = new ProfileTokensApi(client).create(name, lifetime);
      System.err.println(msg.authTokenCreated(token.getName()));
      return new CommandResult<>(token);
    }
  }

  private static Duration parseLifetime(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    Duration lifetime;
    try {
      lifetime = DurationConverter.parseDuration(value.trim());
    } catch (RuntimeException unparseable) {
      throw new CliException(msg.authTokenInvalidExpiry(value));
    }
    if (lifetime == null || lifetime.isZero() || lifetime.isNegative()) {
      throw new CliException(msg.authTokenInvalidExpiry(value));
    }
    return lifetime;
  }
}
