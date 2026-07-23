package com.streamx.cli.commands.project;

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
class ProjectCommandIT extends CliBaseIT {

  private static final String ORG = "so-testorg";

  private StubProjectServer platform;

  private Path getCredentialsPath() {
    return streamxHome.resolve("profiles/default/config/credentials.json");
  }

  @BeforeEach
  void setUp() throws IOException {
    platform = new StubProjectServer();

    Properties properties = new Properties();
    properties.setProperty(PlatformConfig.STREAMX_PLATFORM_URL, platform.getUrl());
    Path configFile = getConfigPath();
    Files.createDirectories(configFile.getParent());
    try (OutputStream out = Files.newOutputStream(configFile)) {
      properties.store(out, null);
    }

    Path credentials = getCredentialsPath();
    Files.createDirectories(credentials.getParent());
    Files.writeString(credentials, """
        {"access_token":"test-access-token","refresh_token":"test-refresh-token",
         "expires_at":%d,"issuer_url":"http://127.0.0.1:1/realms/streamx",
         "client_id":"streamx-cli"}
        """.formatted(Instant.now().plusSeconds(300).getEpochSecond()));
  }

  @AfterEach
  void tearDown() throws IOException {
    if (platform != null) {
      platform.close();
    }
    Files.deleteIfExists(getCredentialsPath());
  }

  @Test
  void shouldListProjects() throws Exception {
    ProcessResult result = exec("project", "list", ORG);

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("GET /api/v1/organizations/" + ORG + "/projects");
    assertThat(result.stdout()).contains("ID", "NAME", "STATE", "DESCRIPTION");
    assertThat(result.stdout()).contains("so-org-web-a1b2c", "web", "Ready");
    assertThat(result.stdout()).contains("so-org-api-d3e4f", "api", "Pending");
  }

  @Test
  void shouldListOnlyProjectIdsWhenQuiet() throws Exception {
    ProcessResult result = exec("project", "list", ORG, "-q");

    result.assertSuccess();
    assertThat(result.stdout()).isEqualTo("so-org-api-d3e4f\nso-org-web-a1b2c\n");
  }

  @Test
  void shouldPrintNothingWhenQuietAndNoProjects() throws Exception {
    platform.returnNoProjects();

    ProcessResult result = exec("project", "list", ORG, "-q");

    result.assertSuccess();
    assertThat(result.stdout()).isEmpty();
  }

  @Test
  void shouldGetProject() throws Exception {
    ProcessResult result = exec("project", "get", ORG, "so-org-web-a1b2c");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("GET /api/v1/organizations/" + ORG + "/projects/so-org-web-a1b2c");
    assertThat(result.stdout()).contains("id          = so-org-web-a1b2c");
  }

  /** The id is server-derived, so create must report the id from the response, not the name. */
  @Test
  void shouldCreateProjectAndReportServerDerivedId() throws Exception {
    ProcessResult result = exec("project", "create", ORG, "web", "--description", "Frontend");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("POST /api/v1/organizations/" + ORG + "/projects");
    assertThat(platform.getRequestBodies().get(0))
        .contains("\"name\":\"web\"", "\"description\":\"Frontend\"");
    assertThat(result.stdout()).contains(msg.projectCreated("web", "so-web"));
  }

  @Test
  void shouldCreateProjectAsJson() throws Exception {
    ProcessResult result = exec("project", "create", ORG, "web", "--output", "json");

    result.assertSuccess();
    assertThat(result.stdout()).contains("\"id\" : \"so-web\"", "\"name\" : \"web\"");
  }

  /** update is a partial patch: the unspecified field is read back from the current project. */
  @Test
  void shouldUpdateOnlyDescriptionKeepingCurrentName() throws Exception {
    ProcessResult result =
        exec("project", "update", ORG, "so-org-web-a1b2c", "--description", "New desc");

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(
        "GET /api/v1/organizations/" + ORG + "/projects/so-org-web-a1b2c",
        "PATCH /api/v1/organizations/" + ORG + "/projects/so-org-web-a1b2c");
    assertThat(platform.getRequestBodies().get(1))
        .contains("\"name\":\"web\"", "\"description\":\"New desc\"");
    assertThat(result.stdout()).contains(msg.projectUpdated("so-org-web-a1b2c"));
  }

  @Test
  void shouldFailUpdateWhenNothingGiven() throws Exception {
    ProcessResult result = exec("project", "update", ORG, "so-org-web-a1b2c");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.projectUpdateNothingToDo());
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void shouldDeleteProject() throws Exception {
    ProcessResult result = exec("project", "delete", ORG, "so-org-web-a1b2c");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("DELETE /api/v1/organizations/" + ORG + "/projects/so-org-web-a1b2c");
    assertThat(result.stdout()).contains(msg.projectDeleted("so-org-web-a1b2c"));
  }

  @Test
  void shouldShowProjectStatusWithComponents() throws Exception {
    ProcessResult result = exec("project", "status", ORG, "so-org-web-a1b2c");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("GET /api/v1/organizations/" + ORG + "/projects/so-org-web-a1b2c/status");
    assertThat(result.stdout()).contains("state = Pending");
    assertThat(result.stdout()).contains("Ready", "Deployed", "web is running");
  }

  @Test
  void shouldListPendingChanges() throws Exception {
    ProcessResult result = exec("project", "pending-changes", ORG, "so-org-web-a1b2c");

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(
        "GET /api/v1/organizations/" + ORG + "/projects/so-org-web-a1b2c/changes/pending");
    assertThat(result.stdout()).contains("web will be created", "image: nginx", "replicas: 2");
  }

  @Test
  void shouldReportNoPendingChanges() throws Exception {
    platform.failWith(200, "[]");

    ProcessResult result = exec("project", "pending-changes", ORG, "so-org-web-a1b2c");

    result.assertSuccess();
    assertThat(result.stdout()).contains(msg.projectPendingChangesEmpty());
  }

  /**
   * A crafted project id must stay one path segment. Unencoded, "so-x/status" would route to the
   * status endpoint instead of delete; the raw path is asserted because getPath() decodes it.
   */
  @Test
  void shouldEncodeProjectIdIntoASinglePathSegment() throws Exception {
    exec("project", "delete", ORG, "so-x/status");

    assertThat(platform.getRawRequests()).containsExactly(
        "DELETE /api/v1/organizations/" + ORG + "/projects/so-x%2Fstatus");
  }
}
