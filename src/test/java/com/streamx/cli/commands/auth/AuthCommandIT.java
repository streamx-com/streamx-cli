package com.streamx.cli.commands.auth;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.auth.AuthConfig;
import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AuthCommandIT extends CliBaseIT {
  private static final String REALM = "streamx";

  private StubOidcServer oidcServer;

  private Path getCredentialsPath() {
    return streamxHome.resolve("config/credentials.json");
  }

  private void writeAuthConfig(String serverUrl) throws IOException {
    Properties properties = new Properties();
    if (serverUrl != null) {
      properties.setProperty(AuthConfig.STREAMX_AUTH_SERVER_URL, serverUrl);
      properties.setProperty(AuthConfig.STREAMX_AUTH_REALM, REALM);
      properties.setProperty(AuthConfig.STREAMX_AUTH_CLIENT_ID, "streamx-cli");
    }
    Path configFile = getConfigPath();
    Files.createDirectories(configFile.getParent());
    try (OutputStream out = Files.newOutputStream(configFile)) {
      properties.store(out, null);
    }
  }

  @BeforeEach
  void cleanState() throws IOException {
    Files.deleteIfExists(getCredentialsPath());
    Files.deleteIfExists(getConfigPath());
  }

  @AfterEach
  void stopServer() {
    if (oidcServer != null) {
      oidcServer.close();
      oidcServer = null;
    }
  }

  @Test
  void shouldPrintUserCodeAndStoreCredentialsOnLogin() throws Exception {
    oidcServer = new StubOidcServer(REALM, 0);
    writeAuthConfig(oidcServer.getServerUrl());

    ProcessResult result = exec("auth", "login", "--no-browser");

    result.assertSuccess();
    assertThat(result.stderr()).contains(StubOidcServer.USER_CODE);
    assertThat(result.stdout()).contains(msg.authLoginSuccess());

    assertThat(getCredentialsPath()).exists();
    String credentials = Files.readString(getCredentialsPath());
    assertThat(credentials).contains(StubOidcServer.ACCESS_TOKEN);
    assertThat(credentials).contains(StubOidcServer.REFRESH_TOKEN);
  }

  @Test
  void shouldKeepPollingWhileAuthorizationIsPending() throws Exception {
    oidcServer = new StubOidcServer(REALM, 2);
    writeAuthConfig(oidcServer.getServerUrl());

    ProcessResult result = exec("auth", "login", "--no-browser");

    result.assertSuccess();
    assertThat(oidcServer.getTokenRequestCount()).isEqualTo(3);
    assertThat(getCredentialsPath()).exists();
  }

  @Test
  void shouldStoreCredentialsOwnerReadableOnly() throws Exception {
    oidcServer = new StubOidcServer(REALM, 0);
    writeAuthConfig(oidcServer.getServerUrl());

    exec("auth", "login", "--no-browser").assertSuccess();

    assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(getCredentialsPath())))
        .isEqualTo("rw-------");
  }

  @Test
  void shouldReportLoggedInUserForWhoami() throws Exception {
    oidcServer = new StubOidcServer(REALM, 0);
    writeAuthConfig(oidcServer.getServerUrl());
    exec("auth", "login", "--no-browser").assertSuccess();

    ProcessResult result = exec("auth", "whoami");

    result.assertSuccess();
    assertThat(result.stdout()).contains("username = " + StubOidcServer.USERNAME);
    assertThat(result.stdout()).contains("email    = " + StubOidcServer.EMAIL);
    assertThat(result.stdout()).contains("subject  = " + StubOidcServer.SUBJECT);
    assertThat(result.stdout()).contains("expires  = ");
  }

  @Test
  void shouldReportWhoamiWhenIdentityProviderIsUnreachable() throws Exception {
    oidcServer = new StubOidcServer(REALM, 0);
    writeAuthConfig(oidcServer.getServerUrl());
    exec("auth", "login", "--no-browser").assertSuccess();

    oidcServer.close();
    oidcServer = null;

    ProcessResult result = exec("auth", "whoami");

    result.assertSuccess();
    assertThat(result.stdout()).contains("username = " + StubOidcServer.USERNAME);
  }

  @Test
  void shouldReportWhoamiAsJson() throws Exception {
    oidcServer = new StubOidcServer(REALM, 0);
    writeAuthConfig(oidcServer.getServerUrl());
    exec("auth", "login", "--no-browser").assertSuccess();

    ProcessResult result = exec("auth", "whoami", "--output", "json");

    result.assertSuccess();
    assertThat(result.stdout()).contains("\"username\" : \"" + StubOidcServer.USERNAME + "\"");
    assertThat(result.stdout()).contains("\"subject\" : \"" + StubOidcServer.SUBJECT + "\"");
  }

  @Test
  void shouldFailWhoamiWhenNotLoggedIn() throws Exception {
    ProcessResult result = exec("auth", "whoami");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.platformNotLoggedIn());
  }

  @Test
  void shouldFailWhenServerUrlNotConfigured() throws Exception {
    writeAuthConfig(null);

    ProcessResult result = exec("auth", "login", "--no-browser");

    result.assertExitCode(1);
    assertThat(result.stderr())
        .contains(msg.authServerUrlNotConfigured(AuthConfig.STREAMX_AUTH_SERVER_URL));
    assertThat(getCredentialsPath()).doesNotExist();
  }

  @Test
  void shouldNotTreatServerErrorAsSuccessfulLogin() throws Exception {
    oidcServer = new StubOidcServer(REALM, 0);
    oidcServer.failTokenWithStatus(503, "{}");
    writeAuthConfig(oidcServer.getServerUrl());

    ProcessResult result = exec("auth", "login", "--no-browser");

    result.assertExitCode(1);
    assertThat(getCredentialsPath()).doesNotExist();
  }

  @Test
  void shouldRejectTokenResponseWithoutAccessToken() throws Exception {
    oidcServer = new StubOidcServer(REALM, 0);
    oidcServer.failTokenWithStatus(200, "{\"token_type\":\"Bearer\"}");
    writeAuthConfig(oidcServer.getServerUrl());

    ProcessResult result = exec("auth", "login", "--no-browser");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.authTokenResponseIncomplete());
    assertThat(getCredentialsPath()).doesNotExist();
  }

  @Test
  void shouldRejectMismatchedDiscoveryIssuer() throws Exception {
    oidcServer = new StubOidcServer(REALM, 0);
    oidcServer.returnWrongIssuer();
    writeAuthConfig(oidcServer.getServerUrl());

    ProcessResult result = exec("auth", "login", "--no-browser");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains("does not match");
    assertThat(getCredentialsPath()).doesNotExist();
  }

  @Test
  void shouldFailWhenLoginIsDenied() throws Exception {
    oidcServer = new StubOidcServer(REALM, 0);
    oidcServer.failTokenWith("access_denied");
    writeAuthConfig(oidcServer.getServerUrl());

    ProcessResult result = exec("auth", "login", "--no-browser");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.authLoginDenied());
    assertThat(getCredentialsPath()).doesNotExist();
  }

  @Test
  void shouldRevokeRefreshTokenAndRemoveCredentialsOnLogout() throws Exception {
    oidcServer = new StubOidcServer(REALM, 0);
    writeAuthConfig(oidcServer.getServerUrl());
    exec("auth", "login", "--no-browser").assertSuccess();

    ProcessResult result = exec("auth", "logout");

    result.assertSuccess();
    assertThat(result.stdout()).contains(msg.authLogoutSuccess());
    assertThat(getCredentialsPath()).doesNotExist();
    assertThat(oidcServer.getRevokedTokens()).containsExactly(StubOidcServer.REFRESH_TOKEN);
  }

  @Test
  void shouldReportNotLoggedInWhenLoggingOutWithoutCredentials() throws Exception {
    writeAuthConfig(null);

    ProcessResult result = exec("auth", "logout");

    result.assertSuccess();
    assertThat(result.stdout()).contains(msg.authLogoutNotLoggedIn());
  }

  @Test
  void shouldRemoveCredentialsEvenWhenIdentityProviderIsUnreachable() throws Exception {
    oidcServer = new StubOidcServer(REALM, 0);
    writeAuthConfig(oidcServer.getServerUrl());
    exec("auth", "login", "--no-browser").assertSuccess();

    oidcServer.close();
    oidcServer = null;

    ProcessResult result = exec("auth", "logout");

    result.assertSuccess();
    assertThat(getCredentialsPath()).doesNotExist();
  }

  @Test
  void shouldClearCorruptCredentialsOnLogout() throws Exception {
    writeAuthConfig(null);
    Path credentials = getCredentialsPath();
    Files.createDirectories(credentials.getParent());
    Files.writeString(credentials, "{ not valid json");

    ProcessResult result = exec("auth", "logout");

    result.assertSuccess();
    assertThat(result.stdout()).contains(msg.authLogoutSuccess());
    assertThat(credentials).doesNotExist();
  }
}
