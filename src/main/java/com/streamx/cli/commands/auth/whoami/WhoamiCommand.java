package com.streamx.cli.commands.auth.whoami;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.auth.AccessTokenClaims;
import com.streamx.cli.auth.Credentials;
import com.streamx.cli.auth.CredentialsStore;
import com.streamx.cli.auth.Identity;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import picocli.CommandLine;

@CommandLine.Command(
    name = "whoami",
    header = "Display the currently logged in user"
)
public class WhoamiCommand extends AbstractCommand<Identity> {

  @Override
  public String getTextOutput(CommandResult<Identity> result) {
    Identity identity = result.getData();
    String expires = identity.expiresAt() == null
        ? "-"
        : identity.expiresAt() + (identity.expired() ? " (expired)" : "");
    return """
        username = %s
        name     = %s
        email    = %s
        subject  = %s
        issuer   = %s
        expires  = %s"""
        .formatted(
            orDash(identity.username()),
            orDash(identity.name()),
            orDash(identity.email()),
            orDash(identity.subject()),
            orDash(identity.issuer()),
            expires);
  }

  private static String orDash(String value) {
    return value == null ? "-" : value;
  }

  @Override
  public CommandResult<Identity> runCommand() {
    Credentials credentials = CredentialsStore.load()
        .orElseThrow(() -> new CliException(msg.platformNotLoggedIn()));

    JsonNode claims = AccessTokenClaims.of(credentials.accessToken());
    Instant expiresAt = credentials.expiresAt();

    return new CommandResult<>(new Identity(
        claims.path("preferred_username").asText(null),
        claims.path("name").asText(null),
        claims.path("email").asText(null),
        claims.path("sub").asText(null),
        claims.path("iss").asText(null),
        expiresAt == null ? null : DateTimeFormatter.ISO_INSTANT.format(expiresAt),
        expiresAt != null && Instant.now().isAfter(expiresAt)));
  }
}
