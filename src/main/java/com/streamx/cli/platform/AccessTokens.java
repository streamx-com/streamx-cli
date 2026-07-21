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

  private AccessTokens() {
  }

  public static String current() {
    Credentials credentials = stored();
    if (!isExpiring(credentials)) {
      return credentials.accessToken();
    }
    return refresh(credentials);
  }

  public static String forceRefresh() {
    return refresh(stored());
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
