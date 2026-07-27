package com.streamx.cli.commands.auth.token.create;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.tokens.ProfileTokensApi;
import com.streamx.cli.platform.tokens.TokenResponse;
import picocli.CommandLine;

@CommandLine.Command(
    name = "create",
    header = "Create a personal access token",
    description = {
        "Prints the token once to standard output - copy it now, it cannot be retrieved again."
    }
)
public class CreateCommand extends AbstractCommand<TokenResponse> {

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "<name>",
      description = "A label to recognize the token later (e.g. ci-github-actions)"
  )
  public String name;

  @Override
  public String getTextOutput(CommandResult<TokenResponse> result) {
    return result.getData().token();
  }

  @Override
  public CommandResult<TokenResponse> runCommand() {
    try (PlatformClients client = PlatformClients.fromConfig()) {
      TokenResponse token = new ProfileTokensApi(client).create(name);
      System.err.println(msg.authTokenCreated(token.name()));
      return new CommandResult<>(token);
    }
  }
}
