package com.streamx.cli.commands.auth.logout;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.auth.Credentials;
import com.streamx.cli.auth.CredentialsStore;
import com.streamx.cli.auth.OidcClient;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import picocli.CommandLine;

@CommandLine.Command(
    name = "logout",
    header = "Log out of StreamX"
)
public class LogoutCommand extends AbstractSilentCommand {
  @Override
  public CommandResult<Void> runCommand() {
    if (!CredentialsStore.exists()) {
      System.out.println(msg.authLogoutNotLoggedIn());
      return new CommandResult<>(null);
    }

    try {
      CredentialsStore.load().ifPresent(LogoutCommand::revoke);
    } catch (CliException expected) {
    }

    CredentialsStore.delete();

    System.out.println(msg.authLogoutSuccess());
    return new CommandResult<>(null);
  }

  private static void revoke(Credentials credentials) {
    if (credentials.refreshToken() != null && credentials.issuerUrl() != null) {
      new OidcClient(
          credentials.issuerUrl(),
          credentials.clientId(),
          credentials.insecure())
          .revokeQuietly(credentials.refreshToken());
    }
  }
}
