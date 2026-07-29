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
import com.streamx.cli.platform.AccessTokens;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.ProfileApi;
import com.streamx.cli.platform.generated.model.Profile;
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
        expires  = %s
        auth     = %s
        token id = %s"""
        .formatted(
            orDash(identity.username()),
            orDash(identity.name()),
            orDash(identity.email()),
            orDash(identity.subject()),
            orDash(identity.issuer()),
            expires,
            AccessTokens.usingPlatformToken() ? "personal access token" : "login session",
            orDash(identity.tokenId()));
  }

  private static String orDash(String value) {
    return value == null ? "-" : value;
  }

  private static Identity identityFromPlatform() {
    Profile profile;
    try (PlatformClients client = PlatformClients.fromConfig()) {
      profile = new ProfileApi(client).get();
    }
    if (profile == null) {
      throw new CliException(msg.authTokenIdentityUnavailable());
    }
    return new Identity(
        firstNonBlank(profile.getDisplayName(), profile.getEmail()),
        firstNonBlank(profile.getDisplayName(),
            join(profile.getFirstName(), profile.getLastName())),
        profile.getEmail(),
        profile.getUserId(),
        null,
        null,
        false,
        AccessTokens.platformTokenId().orElse(null));
  }

  private static String join(String first, String last) {
    return firstNonBlank(((first == null ? "" : first) + " "
        + (last == null ? "" : last)).trim(), null);
  }

  private static String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  @Override
  public CommandResult<Identity> runCommand() {
    if (AccessTokens.usingPlatformToken()) {
      // A personal access token is opaque and carries no claims: ask the platform who it acts as.
      return new CommandResult<>(identityFromPlatform());
    }

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
        expiresAt != null && Instant.now().isAfter(expiresAt),
        null));
  }
}
