package com.streamx.cli.commands.auth;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.auth.AuthConfig;
import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@QuarkusTest
@EnabledIfSystemProperty(
    named = AuthTestEndpoints.SERVER_URL_PROPERTY,
    matches = ".+",
    disabledReason = "Set -D" + AuthTestEndpoints.SERVER_URL_PROPERTY + " to run against a real IdP"
)
class AuthCommandRealKeycloakIT extends CliBaseIT {
  private static final Pattern VERIFICATION_LINK =
      Pattern.compile("(https?://\\S*user_code=[A-Za-z0-9-]+)");

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private Path getCredentialsPath() {
    return streamxHome.resolve("contexts/default/config/credentials.json");
  }

  @BeforeEach
  void configureAgainstRealIdentityProvider() throws IOException {
    Files.deleteIfExists(getCredentialsPath());

    Properties properties = new Properties();
    properties.setProperty(AuthConfig.STREAMX_AUTH_SERVER_URL, AuthTestEndpoints.serverUrl());
    properties.setProperty(AuthConfig.STREAMX_AUTH_REALM, AuthTestEndpoints.realm());
    properties.setProperty(AuthConfig.STREAMX_AUTH_CLIENT_ID, AuthTestEndpoints.clientId());
    properties.setProperty(
        AuthConfig.STREAMX_AUTH_INSECURE, String.valueOf(AuthTestEndpoints.insecure()));

    Path configFile = getConfigPath();
    Files.createDirectories(configFile.getParent());
    try (OutputStream out = Files.newOutputStream(configFile)) {
      properties.store(out, null);
    }
  }

  @Test
  void shouldCompleteDeviceFlowAgainstRealKeycloak() throws Exception {
    AsyncProcessHandle login = execAsync("auth", "login", "--no-browser");

    String verificationUri = awaitVerificationUri(login);
    try (KeycloakDeviceApprover approver =
             new KeycloakDeviceApprover(AuthTestEndpoints.insecure())) {
      approver.approve(
          verificationUri, AuthTestEndpoints.username(), AuthTestEndpoints.password());
    }

    Awaitility.await("cli stores credentials after approval")
        .atMost(60, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .until(() -> Files.exists(getCredentialsPath()));

    login.interruptAndJoin(TimeUnit.SECONDS.toMillis(10));
    assertThat(login.getStdout()).contains(msg.authLoginSuccess());

    JsonNode claims = accessTokenClaims();
    assertThat(claims.path("iss").asText())
        .isEqualTo(AuthTestEndpoints.serverUrl() + "/realms/" + AuthTestEndpoints.realm());
    assertThat(claims.path("azp").asText()).isEqualTo(AuthTestEndpoints.clientId());
    assertThat(claims.path("preferred_username").asText())
        .isEqualTo(AuthTestEndpoints.username());
  }

  @Test
  void shouldRevokeRealRefreshTokenOnLogout() throws Exception {
    AsyncProcessHandle login = execAsync("auth", "login", "--no-browser");
    String verificationUri = awaitVerificationUri(login);
    try (KeycloakDeviceApprover approver =
             new KeycloakDeviceApprover(AuthTestEndpoints.insecure())) {
      approver.approve(
          verificationUri, AuthTestEndpoints.username(), AuthTestEndpoints.password());
    }
    Awaitility.await()
        .atMost(60, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .until(() -> Files.exists(getCredentialsPath()));
    login.interruptAndJoin(TimeUnit.SECONDS.toMillis(10));

    String refreshToken = credentials().path("refresh_token").asText();
    assertThat(refreshTokenAccepted(refreshToken))
        .as("refresh token should work before logout")
        .isTrue();

    exec("auth", "logout").assertSuccess();

    assertThat(getCredentialsPath()).doesNotExist();
    assertThat(refreshTokenAccepted(refreshToken))
        .as("refresh token should be revoked at the identity provider after logout")
        .isFalse();
  }

  private String awaitVerificationUri(AsyncProcessHandle login) {
    return Awaitility.await("cli prints the verification link")
        .atMost(60, TimeUnit.SECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .until(() -> {
          Matcher matcher = VERIFICATION_LINK.matcher(login.getStderr() + "\n" + login.getStdout());
          return matcher.find() ? matcher.group(1) : null;
        }, uri -> uri != null);
  }

  private JsonNode credentials() throws IOException {
    return MAPPER.readTree(Files.readString(getCredentialsPath()));
  }

  private JsonNode accessTokenClaims() throws IOException {
    String payload = credentials().path("access_token").asText().split("\\.")[1];
    return MAPPER.readTree(Base64.getUrlDecoder().decode(payload));
  }

  private boolean refreshTokenAccepted(String refreshToken) throws Exception {
    String tokenEndpoint = AuthTestEndpoints.serverUrl()
        + "/realms/" + AuthTestEndpoints.realm()
        + "/protocol/openid-connect/token";
    String form = "grant_type=refresh_token"
        + "&client_id=" + AuthTestEndpoints.clientId()
        + "&refresh_token=" + java.net.URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

    try (var client = new TestHttpClient(AuthTestEndpoints.insecure())) {
      return client.postForm(tokenEndpoint, form).contains("access_token");
    }
  }
}
