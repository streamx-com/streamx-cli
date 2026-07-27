package com.streamx.cli.commands.auth.token.revoke;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.TokenIdCompletionCandidates;
import com.streamx.cli.platform.tokens.ProfileTokensApi;
import picocli.CommandLine;

@CommandLine.Command(
    name = "revoke",
    header = "Revoke a personal access token"
)
public class RevokeCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "<id>",
      description = "The token id (from 'streamx auth token list')",
      completionCandidates = TokenIdCompletionCandidates.class
  )
  public String id;

  @Override
  public CommandResult<Void> runCommand() {
    try (PlatformClients client = PlatformClients.fromConfig()) {
      new ProfileTokensApi(client).revoke(id);
    }
    System.out.println(msg.authTokenRevoked());
    return new CommandResult<>(null);
  }
}
