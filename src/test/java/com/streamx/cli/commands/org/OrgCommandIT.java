package com.streamx.cli.commands.org;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

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
class OrgCommandIT extends CliBaseIT {
  private StubPlatformServer platform;

  private Path getCredentialsPath() {
    return streamxHome.resolve("profiles/default/config/credentials.json");
  }

  private void writeCredentials(Instant expiresAt) throws IOException {
    Path path = getCredentialsPath();
    Files.createDirectories(path.getParent());
    Files.writeString(path, """
        {"access_token":"test-access-token","refresh_token":"test-refresh-token",
         "expires_at":%d,"issuer_url":"http://127.0.0.1:1/realms/streamx",
         "client_id":"streamx-cli"}
        """.formatted(expiresAt.getEpochSecond()));
  }

  @BeforeEach
  void setUp() throws IOException {
    platform = new StubPlatformServer();

    Properties properties = new Properties();
    properties.setProperty(PlatformConfig.STREAMX_PLATFORM_URL, platform.getUrl());
    Path configFile = getConfigPath();
    Files.createDirectories(configFile.getParent());
    try (OutputStream out = Files.newOutputStream(configFile)) {
      properties.store(out, null);
    }

    writeCredentials(Instant.now().plusSeconds(300));
  }

  @AfterEach
  void tearDown() throws IOException {
    if (platform != null) {
      platform.close();
    }
    Files.deleteIfExists(getCredentialsPath());
  }

  @Test
  void shouldListOrganizations() throws Exception {
    ProcessResult result = exec("org", "list");

    result.assertSuccess();
    assertThat(result.stdout()).contains("acme", "Acme", "owner", "globex", "Globex", "view");
    assertThat(result.stdout()).contains("ID", "NAME", "ROLE", "PROJECTS", "STATE");
  }

  @Test
  void shouldSendBearerTokenOnEveryRequest() throws Exception {
    exec("org", "list").assertSuccess();

    assertThat(platform.getAuthorizationHeaders())
        .containsExactly("Bearer test-access-token");
  }

  @Test
  void completeOrgIdsListsIdsOnePerLine() throws Exception {
    ProcessResult result = exec("__complete-org-ids");

    result.assertSuccess();
    assertThat(result.stdout().strip().lines()).containsExactly("acme", "globex");
  }

  @Test
  void completeOrgIdsIsSilentWhenNotLoggedIn() throws Exception {
    Files.deleteIfExists(getCredentialsPath());

    ProcessResult result = exec("__complete-org-ids");

    result.assertSuccess();
    assertThat(result.stdout().strip()).isEmpty();
  }

  @Test
  void shouldListOnlyOrganizationIdsWhenQuiet() throws Exception {
    ProcessResult result = exec("org", "list", "--quiet");

    result.assertSuccess();
    assertThat(result.stdout()).isEqualTo("acme\nglobex\n");
  }

  @Test
  void shouldPrintNothingWhenQuietAndNoOrganizations() throws Exception {
    platform.returnNoOrganizations();

    ProcessResult result = exec("org", "list", "-q");

    result.assertSuccess();
    assertThat(result.stdout()).isEmpty();
  }

  @Test
  void shouldListOrganizationsAsJson() throws Exception {
    ProcessResult result = exec("org", "list", "--output", "json");

    result.assertSuccess();
    assertThat(result.stdout()).contains("\"id\" : \"acme\"", "\"role\" : \"owner\"");
  }

  @Test
  void shouldGetOrganization() throws Exception {
    ProcessResult result = exec("org", "get", "acme");

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly("GET /api/v1/organizations/acme");
    assertThat(result.stdout()).contains("id             = acme");
  }

  @Test
  void shouldCreateOrganization() throws Exception {
    ProcessResult result = exec("org", "create", "my-org");

    result.assertSuccess();
    assertThat(platform.getCreatedNames()).containsExactly("my-org");
    assertThat(result.stdout()).contains(msg.orgCreated("my-org"));
  }

  @Test
  void shouldDeleteOrganizationAfterTypedConfirmation() throws Exception {
    ProcessResult result = execWithStdin("acme\n", "org", "delete", "acme");

    result.assertSuccess();
    assertThat(platform.getDeletedIds()).containsExactly("acme");
    assertThat(result.stdout()).contains(msg.orgDeleted("acme"));
  }

  @Test
  void shouldDeleteOrganizationWithForceWithoutPrompting() throws Exception {
    ProcessResult result = exec("org", "delete", "-f", "acme");

    result.assertSuccess();
    assertThat(platform.getDeletedIds()).containsExactly("acme");
  }

  @Test
  void shouldCancelDeletionWhenConfirmationDoesNotMatch() throws Exception {
    ProcessResult result = execWithStdin("wrong-id\n", "org", "delete", "acme");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.deleteConfirmMismatch("acme"));
    assertThat(platform.getDeletedIds()).isEmpty();
  }

  @Test
  void shouldRequireForceWhenNoInputIsAvailable() throws Exception {
    ProcessResult result = exec("org", "delete", "acme");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains("--force");
    assertThat(platform.getDeletedIds()).isEmpty();
  }

  @Test
  void shouldFailWhenNotLoggedIn() throws Exception {
    Files.deleteIfExists(getCredentialsPath());

    ProcessResult result = exec("org", "list");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.platformNotLoggedIn());
  }

  @Test
  void shouldFailWhenPlatformUrlNotConfigured() throws Exception {
    try (OutputStream out = Files.newOutputStream(getConfigPath())) {
      new Properties().store(out, null);
    }

    ProcessResult result = exec("org", "list");

    result.assertExitCode(1);
    assertThat(result.stderr())
        .contains(msg.platformUrlNotConfigured(PlatformConfig.STREAMX_PLATFORM_URL));
  }

  /**
   * A crafted ID must not retarget the request at another endpoint: unencoded, this would reach
   * the member-removal route. The raw path is asserted because getPath() decodes, hiding the
   * difference.
   */
  @Test
  void shouldEncodeOrganizationIdIntoASinglePathSegment() throws Exception {
    exec("org", "delete", "-f", "so-x/users/alice@example.com");

    assertThat(platform.getRawRequests()).containsExactly(
        "DELETE /api/v1/organizations/so-x%2Fusers%2Falice%40example.com");
  }

  @Test
  void shouldReportMissingOrForbiddenOrganizationFor404() throws Exception {
    platform.failWith(404, "");

    ProcessResult result = exec("org", "get", "nope");

    result.assertExitCode(1);
    assertThat(result.stderr())
        .contains(msg.platformNotFoundOrForbidden("/api/v1/organizations/nope"));
  }

  @Test
  void shouldSurfaceServerValidationViolations() throws Exception {
    platform.failWith(400, """
        {"errorMessage":"Validation failed","errorCode":400,
         "violations":[{"field":"name","message":"must not be blank"}]}
        """);

    ProcessResult result = exec("org", "create", "bad name");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains("Validation failed");
    assertThat(result.stderr()).contains("name: must not be blank");
  }

  @Test
  void shouldRefuseToUseAnExpiredSessionThatCannotBeRefreshed() throws Exception {
    writeCredentials(Instant.now().minusSeconds(60));

    ProcessResult result = exec("org", "list");

    result.assertExitCode(1);
    assertThat(result.stderr()).isNotEmpty();
    assertThat(platform.getRequests()).isEmpty();
  }
}
