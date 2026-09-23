package com.streamx.cli.auth;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.auth.OidcClient.Endpoints;
import com.streamx.cli.auth.OidcClient.Response;
import com.streamx.cli.framework.CliException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class OidcDeviceFlow {
  private static final String DEVICE_CODE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";
  private static final int DEFAULT_POLL_INTERVAL_SECONDS = 5;

  private static final int SLOW_DOWN_INCREMENT_SECONDS = 5;

  private final OidcClient client;

  public OidcDeviceFlow(OidcClient client) {
    this.client = client;
  }

  public record DeviceAuthorization(
      String deviceCode,
      String userCode,
      String verificationUri,
      String verificationUriComplete,
      int intervalSeconds,
      Instant expiresAt,
      Pkce pkce
  ) {
  }

  public DeviceAuthorization requestDeviceAuthorization(Endpoints endpoints) {
    return requestDeviceAuthorization(endpoints, Scopes.LOGIN);
  }

  public DeviceAuthorization requestDeviceAuthorization(Endpoints endpoints, String scope) {
    String url = endpoints.deviceAuthorizationEndpoint();
    if (url == null) {
      throw new CliException(msg.authDeviceFlowUnsupported(url));
    }
    Pkce pkce = Pkce.generate();
    Map<String, String> form = Map.of(
        "client_id", client.clientId(),
        "scope", scope,
        "code_challenge", pkce.challenge(),
        "code_challenge_method", Pkce.METHOD);
    JsonNode node = client.send(OidcClient.postForm(url, form), url, true);

    int interval = node.path("interval").asInt(DEFAULT_POLL_INTERVAL_SECONDS);
    long expiresIn = node.path("expires_in").asLong(0);

    return new DeviceAuthorization(
        node.path("device_code").asText(null),
        node.path("user_code").asText(null),
        node.path("verification_uri").asText(null),
        node.path("verification_uri_complete").asText(null),
        interval > 0 ? interval : DEFAULT_POLL_INTERVAL_SECONDS,
        Instant.now().plusSeconds(expiresIn),
        pkce
    );
  }

  public Credentials pollForToken(Endpoints endpoints, DeviceAuthorization authorization) {
    String url = endpoints.tokenEndpoint();
    Map<String, String> form = Map.of(
        "grant_type", DEVICE_CODE_GRANT,
        "device_code", authorization.deviceCode(),
        "client_id", client.clientId(),
        "code_verifier", authorization.pkce().verifier()
    );

    int intervalSeconds = authorization.intervalSeconds();

    while (Instant.now().isBefore(authorization.expiresAt())) {
      sleepSeconds(intervalSeconds);

      Response response = client.exchange(OidcClient.postForm(url, form), url);
      String error = response.body().path("error").asText(null);

      if (response.isSuccess()) {
        return client.toCredentials(response.body(), null);
      }
      if (error == null) {
        throw new CliException(msg.authRequestFailedWithStatus(url, response.statusCode()));
      }

      if ("slow_down".equals(error)) {
        intervalSeconds += SLOW_DOWN_INCREMENT_SECONDS;
      } else if ("access_denied".equals(error)) {
        throw new CliException(msg.authLoginDenied());
      } else if ("expired_token".equals(error)) {
        throw new CliException(msg.authLoginExpired());
      } else if (!"authorization_pending".equals(error)) {
        throw new CliException(msg.authLoginFailed(error));
      }
    }

    throw new CliException(msg.authLoginExpired());
  }

  private void sleepSeconds(int seconds) {
    try {
      Thread.sleep(Duration.ofSeconds(seconds).toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CliException(msg.authLoginInterrupted(), e);
    }
  }
}
