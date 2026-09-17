package com.streamx.cli.commands.auth.login;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.auth.AuthConfig;
import com.streamx.cli.auth.Credentials;
import com.streamx.cli.auth.CredentialsStore;
import com.streamx.cli.auth.InteractiveGrant;
import com.streamx.cli.auth.OidcClient;
import com.streamx.cli.auth.Scopes;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import java.util.Optional;
import picocli.CommandLine;

@CommandLine.Command(
    name = "login",
    header = "Log in to StreamX",
    description = {
        "Uses the browser (authorization code + PKCE) when a local browser is available,",
        "and falls back to the device flow over SSH or with --no-browser."
    }
)
public class LoginCommand extends AbstractSilentCommand {

  @CommandLine.Option(
      names = "--no-browser",
      description = "Use the device flow instead of opening a local browser "
          + "(for SSH sessions and headless machines)"
  )
  public boolean noBrowser;

  @Override
  public CommandResult<Void> runCommand() {
    AuthConfig config = AuthConfig.load();
    OidcClient client = InteractiveGrant.clientFromConfig(config);

    Credentials credentials = InteractiveGrant.run(
        client, client.discover(), Scopes.LOGIN, noBrowser);

    Optional<Credentials> previous = CredentialsStore.load();
    CredentialsStore.save(credentials);
    previous.ifPresent(LoginCommand::revokePrevious);

    System.out.println(msg.authLoginSuccess());
    return new CommandResult<>(null);
  }

  private static void revokePrevious(Credentials previous) {
    if (previous.refreshToken() == null || previous.issuerUrl() == null) {
      return;
    }
    new OidcClient(
        previous.issuerUrl(),
        previous.clientId(),
        previous.insecure())
        .revokeQuietly(previous.refreshToken());
  }
}
