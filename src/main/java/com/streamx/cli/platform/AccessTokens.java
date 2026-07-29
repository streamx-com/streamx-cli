package com.streamx.cli.platform;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.auth.Credentials;
import com.streamx.cli.auth.CredentialsStore;
import com.streamx.cli.auth.OidcClient;
import com.streamx.cli.framework.CliException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class AccessTokens {
  private static final Duration EXPIRY_SKEW = Duration.ofSeconds(30);

  public static final String STREAMX_PLATFORM_TOKEN = "STREAMX_PLATFORM_TOKEN";

  private AccessTokens() {
  }

  public static String current() {
    String envToken = platformTokenOverride();
    if (envToken != null) {
      return envToken;
    }
    Credentials credentials = stored();
    if (!isExpiring(credentials)) {
      return credentials.accessToken();
    }
    return refresh(credentials);
  }

  public static String forceRefresh() {
    String envToken = platformTokenOverride();
    if (envToken != null) {
      // A personal access token cannot be refreshed; a 401 means it is invalid or revoked.
      return envToken;
    }
    return refresh(stored());
  }

  public static boolean usingPlatformToken() {
    return platformTokenOverride() != null;
  }

  public static void requireInteractiveSession() {
    if (usingPlatformToken()) {
      throw new CliException(msg.authTokenNeedsLoginSession(STREAMX_PLATFORM_TOKEN));
    }
  }

  public static Optional<String> platformTokenId() {
    String token = platformTokenOverride();
    if (token == null) {
      return Optional.empty();
    }
    String[] parts = token.split("_");
    return parts.length == 5 && "sxp".equals(parts[0]) && !parts[2].isBlank()
        ? Optional.of(parts[2])
        : Optional.empty();
  }

  private static String platformTokenOverride() {
    String value = System.getenv(STREAMX_PLATFORM_TOKEN);
    if (value == null || value.isBlank()) {
      value = System.getProperty(STREAMX_PLATFORM_TOKEN);
    }
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static Credentials stored() {
    return CredentialsStore.load()
        .orElseThrow(() -> new CliException(msg.platformNotLoggedIn()));
  }

  private static String refresh(Credentials credentials) {
    if (credentials.refreshToken() == null || credentials.issuerUrl() == null) {
      throw new CliException(msg.authSessionExpired());
    }

    // Refresh under the TLS policy the session was created with, not the current config.
    Credentials refreshed = new OidcClient(
        credentials.issuerUrl(),
        credentials.clientId(),
        credentials.insecure())
        .refresh(credentials.refreshToken());

    CredentialsStore.save(refreshed);
    return refreshed.accessToken();
  }

  private static boolean isExpiring(Credentials credentials) {
    return Optional.ofNullable(credentials.expiresAt())
        .map(expiry -> Instant.now().plus(EXPIRY_SKEW).isAfter(expiry))
        .orElse(true);
  }
}
