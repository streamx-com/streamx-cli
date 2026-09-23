package com.streamx.cli.commands.auth;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.auth.OidcAuthCodeFlow;
import com.streamx.cli.auth.OidcClient;
import com.streamx.cli.auth.OidcClient.Endpoints;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthCodeFlowIT {

  private static final String REALM = "streamx";

  private StubOidcServer oidcServer;
  private Endpoints endpoints;

  @BeforeEach
  void setUp() throws IOException {
    oidcServer = new StubOidcServer(REALM, 0);
    endpoints = new OidcClient(
        oidcServer.getServerUrl() + "/realms/" + REALM, "streamx-cli", false).discover();
  }

  @AfterEach
  void tearDown() {
    if (oidcServer != null) {
      oidcServer.close();
    }
  }

  private static void simulateBrowser(String authorizationUrl) {
    HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    try {
      HttpResponse<Void> response = http.send(
          HttpRequest.newBuilder(URI.create(authorizationUrl)).GET().build(),
          HttpResponse.BodyHandlers.discarding());
      String location = response.headers().firstValue("Location").orElseThrow();
      http.send(HttpRequest.newBuilder(URI.create(location)).GET().build(),
          HttpResponse.BodyHandlers.discarding());
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void shouldCompletePkceLoginWithoutACode() {
    OidcClient client = new OidcClient(
        oidcServer.getServerUrl() + "/realms/" + REALM, "streamx-cli", false);

    var flow = new OidcAuthCodeFlow(client, AuthCodeFlowIT::simulateBrowser);
    var credentials = flow.login(endpoints);

    assertThat(credentials.accessToken()).isEqualTo(StubOidcServer.ACCESS_TOKEN);
    assertThat(credentials.refreshToken()).isEqualTo(StubOidcServer.REFRESH_TOKEN);
  }

  @Test
  void shouldSendPkceChallengeAndVerifier() {
    OidcClient client = new OidcClient(
        oidcServer.getServerUrl() + "/realms/" + REALM, "streamx-cli", false);

    new OidcAuthCodeFlow(client, AuthCodeFlowIT::simulateBrowser).login(endpoints);

    assertThat(oidcServer.getLastAuthorizationRequest())
        .containsEntry("code_challenge_method", "S256")
        .containsKey("code_challenge");
    assertThat(oidcServer.getLastAuthorizationRequest().get("redirect_uri"))
        .startsWith("http://127.0.0.1:");
    assertThat(oidcServer.getLastTokenRequestBody())
        .contains("grant_type=authorization_code")
        .contains("code_verifier=")
        .contains("code=" + StubOidcServer.AUTH_CODE);
  }

  @Test
  void shouldFailWhenAuthorizationIsDenied() {
    oidcServer.denyAuthorization();
    OidcClient client = new OidcClient(
        oidcServer.getServerUrl() + "/realms/" + REALM, "streamx-cli", false);

    OidcAuthCodeFlow flow = new OidcAuthCodeFlow(client, AuthCodeFlowIT::simulateBrowser);

    assertThat(org.junit.jupiter.api.Assertions.assertThrows(
        RuntimeException.class, () -> flow.login(endpoints)).getMessage())
        .contains(msg.authLoginDenied());
  }

  @Test
  void shouldSignalBrowserUnavailableWhenLauncherFails() {
    OidcClient client = new OidcClient(
        oidcServer.getServerUrl() + "/realms/" + REALM, "streamx-cli", false);

    OidcAuthCodeFlow.BrowserLauncher failing = url -> {
      throw new java.io.IOException("no browser");
    };
    OidcAuthCodeFlow flow = new OidcAuthCodeFlow(client, failing);

    org.junit.jupiter.api.Assertions.assertThrows(
        OidcAuthCodeFlow.BrowserUnavailableException.class, () -> flow.login(endpoints));
  }
}
