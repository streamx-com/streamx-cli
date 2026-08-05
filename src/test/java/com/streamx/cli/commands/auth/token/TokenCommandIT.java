package com.streamx.cli.commands.auth.token;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.platform.AccessTokens;
import com.streamx.cli.platform.PlatformConfig;
import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TokenCommandIT extends CliBaseIT {

  private StubTokensServer platform;

  private Path getCredentialsPath() {
    return streamxHome.resolve("contexts/default/config/credentials.json");
  }

  private void writeCredentials() throws IOException {
    Path path = getCredentialsPath();
    Files.createDirectories(path.getParent());
    Files.writeString(path, """
        {"access_token":"test-access-token","refresh_token":"test-refresh-token",
         "expires_at":%d,"issuer_url":"http://127.0.0.1:1/realms/streamx",
         "client_id":"streamx-cli"}
        """.formatted(Instant.now().plusSeconds(300).getEpochSecond()));
  }

  @BeforeEach
  void setUp() throws IOException {
    platform = new StubTokensServer();

    Properties properties = new Properties();
    properties.setProperty(PlatformConfig.STREAMX_PLATFORM_URL, platform.getUrl());
    Path configFile = getConfigPath();
    Files.createDirectories(configFile.getParent());
    try (OutputStream out = Files.newOutputStream(configFile)) {
      properties.store(out, null);
    }
    writeCredentials();
  }

  @AfterEach
  void tearDown() throws IOException {
    clearEnv(AccessTokens.STREAMX_PLATFORM_TOKEN);
    if (platform != null) {
      platform.close();
    }
    Files.deleteIfExists(getCredentialsPath());
  }

  @Test
  void shouldPrintOnlyTheTokenOnStdoutWhenCreating() throws Exception {
    ProcessResult result = exec("auth", "token", "create", "ci");

    result.assertSuccess();
    // The token is the machine output: stdout must be pipeable, the reminder goes to stderr.
    assertThat(result.stdout().strip()).isEqualTo(StubTokensServer.TOKEN);
    assertThat(result.stderr()).contains(msg.authTokenCreated("ci"));
    assertThat(platform.getRequests()).contains("POST /api/v1/profile/tokens");
    assertThat(platform.getRequestBodies()).anyMatch(body -> body.contains("\"name\":\"ci\""));
  }

  @Test
  void shouldListTokens() throws Exception {
    ProcessResult result = exec("auth", "token", "list");

    result.assertSuccess();
    assertThat(result.stdout()).contains("ID", "NAME", "CREATED", "LAST USED", "EXPIRES");
    assertThat(result.stdout()).contains(StubTokensServer.TOKEN_ID, "ci", "never");
    assertThat(result.stdout()).contains(StubTokensServer.EXPIRED_TOKEN_ID, "expired");
  }

  @Test
  void shouldSendTheRequestedExpiry() throws Exception {
    ProcessResult result = exec("auth", "token", "create", "ci", "--expires-in", "30d");

    result.assertSuccess();
    // 30d parses to a Duration whose ISO-8601 form is PT720H - the wire format of expiresIn.
    assertThat(platform.getRequestBodies())
        .anyMatch(body -> body.contains("\"expiresIn\":\"PT720H\""));
  }

  @Test
  void shouldRejectAnInvalidExpiryBeforeCallingThePlatform() throws Exception {
    ProcessResult result = exec("auth", "token", "create", "ci", "--expires-in", "soon");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.authTokenInvalidExpiry("soon"));
    assertThat(platform.getRequests()).doesNotContain("POST /api/v1/profile/tokens");
  }

  @Test
  void shouldReportEmptyTokenList() throws Exception {
    platform.returnNoTokens();

    ProcessResult result = exec("auth", "token", "list");

    result.assertSuccess();
    assertThat(result.stdout()).contains(msg.authTokenListEmpty());
  }

  @Test
  void shouldRevokeToken() throws Exception {
    ProcessResult result = exec("auth", "token", "revoke", StubTokensServer.TOKEN_ID);

    result.assertSuccess();
    assertThat(result.stdout()).contains(msg.authTokenRevoked());
    assertThat(platform.getRequests())
        .contains("DELETE /api/v1/profile/tokens/" + StubTokensServer.TOKEN_ID);
  }

  @Test
  void shouldSendPersonalAccessTokenAsBearerWithoutStoredLogin() throws Exception {
    Files.deleteIfExists(getCredentialsPath());
    setEnv(AccessTokens.STREAMX_PLATFORM_TOKEN, StubTokensServer.TOKEN);

    ProcessResult result = exec("auth", "whoami");

    result.assertSuccess();
    assertThat(platform.getAuthorizationHeaders()).contains("Bearer " + StubTokensServer.TOKEN);
  }

  @Test
  void shouldPreferPersonalAccessTokenOverStoredSession() throws Exception {
    setEnv(AccessTokens.STREAMX_PLATFORM_TOKEN, StubTokensServer.TOKEN);

    exec("auth", "whoami").assertSuccess();

    assertThat(platform.getAuthorizationHeaders()).contains("Bearer " + StubTokensServer.TOKEN);
    assertThat(platform.getAuthorizationHeaders())
        .doesNotContain("Bearer test-access-token");
  }

  @Test
  void shouldRefuseTokenManagementWhenAmbientCredentialIsAToken() throws Exception {
    setEnv(AccessTokens.STREAMX_PLATFORM_TOKEN, StubTokensServer.TOKEN);

    for (String[] command : new String[][] {
        {"auth", "token", "list"},
        {"auth", "token", "create", "ci"},
        {"auth", "token", "revoke", StubTokensServer.TOKEN_ID}}) {
      ProcessResult result = exec(command);

      result.assertExitCode(1);
      assertThat(result.stderr())
          .contains(msg.authTokenNeedsLoginSession(AccessTokens.STREAMX_PLATFORM_TOKEN));
    }
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void shouldNotPrintTheTokenOutsideItsOwnField() throws Exception {
    ProcessResult result = exec("auth", "token", "create", "-o", "json", "ci");

    result.assertSuccess();
    assertThat(result.stdout()).contains("\"token\"");
    assertThat(result.stdout()).contains(StubTokensServer.TOKEN);
    assertThat(result.stderr()).doesNotContain(StubTokensServer.TOKEN);
  }

  @Test
  void shouldReportAnUnknownTokenIdOnRevoke() throws Exception {
    platform.failWith(404, "");

    ProcessResult result = exec("auth", "token", "revoke", "0".repeat(32));

    result.assertExitCode(1);
    assertThat(result.stdout()).doesNotContain(msg.authTokenRevoked());
  }

  @Test
  void shouldReportIdentityForWhoamiWithoutStoredLogin() throws Exception {
    Files.deleteIfExists(getCredentialsPath());
    setEnv(AccessTokens.STREAMX_PLATFORM_TOKEN, StubTokensServer.TOKEN);

    ProcessResult result = exec("auth", "whoami");

    result.assertSuccess();
    assertThat(result.stdout()).contains("Ci Bot", "ci@streamx.com", "user-1");
    assertThat(result.stdout()).contains("personal access token");
  }

  @Test
  void shouldListTokenIdsOnlyWhenQuiet() throws Exception {
    ProcessResult result = exec("auth", "token", "list", "--quiet");

    result.assertSuccess();
    assertThat(result.stdout().lines().filter(line -> !line.isBlank()))
        .containsExactly(StubTokensServer.TOKEN_ID, StubTokensServer.EXPIRED_TOKEN_ID);
    assertThat(result.stdout()).doesNotContain("NAME", "CREATED");
  }

  @Test
  void shouldNotRetryOrSuggestLoginWhenTokenIsRejected() throws Exception {
    Files.deleteIfExists(getCredentialsPath());
    setEnv(AccessTokens.STREAMX_PLATFORM_TOKEN, StubTokensServer.TOKEN);
    platform.failWith(401, "");

    ProcessResult result = exec("auth", "whoami");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.platformTokenUnauthorized());
    // A token cannot be refreshed, so the rejected credential must not be sent twice.
    assertThat(platform.getRequests()).hasSize(1);
  }

  @Test
  void shouldSurfaceTheServerExplanationOnRefusal() throws Exception {
    setEnv(AccessTokens.STREAMX_PLATFORM_TOKEN, StubTokensServer.TOKEN);
    platform.failWith(403, "{\"errorMessage\":\"Token owner is no longer active\"}");

    ProcessResult result = exec("auth", "whoami");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains("Token owner is no longer active");
  }
}
