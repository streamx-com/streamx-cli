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
    Files.deleteIfExists(streamxHome.resolve("profiles/default/current-org"));
    Files.deleteIfExists(streamxHome.resolve("profiles/default/current-project"));
  }

  @Test
  void shouldListProjects() throws Exception {
    ProcessResult result = exec("project", "list", "--org", ORG);

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("GET /api/v1/organizations/" + ORG + "/projects");
    assertThat(result.stdout()).contains("ID", "NAME", "STATE", "DESCRIPTION");
    assertThat(result.stdout()).contains("so-org-web-a1b2c", "web", "Ready");
    assertThat(result.stdout()).contains("so-org-api-d3e4f", "api", "Pending");
  }

  @Test
  void completeProjectIdsListsIdsOfTheGivenOrg() throws Exception {
    ProcessResult result = exec("__complete-project-ids", ORG);

    result.assertSuccess();
    assertThat(result.stdout().strip().lines())
        .containsExactly("so-org-api-d3e4f", "so-org-web-a1b2c");
  }

  @Test
  void completeProjectIdsIsSilentWithoutOrgArgument() throws Exception {
    ProcessResult result = exec("__complete-project-ids");

    result.assertSuccess();
    assertThat(result.stdout().strip()).isEmpty();
  }

  @Test
  void shouldListOnlyProjectIdsWhenQuiet() throws Exception {
    ProcessResult result = exec("project", "list", "--org", ORG, "-q");

    result.assertSuccess();
    assertThat(result.stdout()).isEqualTo("so-org-api-d3e4f\nso-org-web-a1b2c\n");
  }

  @Test
  void shouldPrintNothingWhenQuietAndNoProjects() throws Exception {
    platform.returnNoProjects();

    ProcessResult result = exec("project", "list", "--org", ORG, "-q");

    result.assertSuccess();
    assertThat(result.stdout()).isEmpty();
  }

  @Test
  void shouldGetProject() throws Exception {
    ProcessResult result = exec("project", "get", "so-org-web-a1b2c", "--org", ORG);

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("GET /api/v1/organizations/" + ORG + "/projects/so-org-web-a1b2c");
    assertThat(result.stdout()).contains("id          = so-org-web-a1b2c");
  }

  /** The id is server-derived, so create must report the id from the response, not the name. */
  @Test
  void shouldCreateProjectAndReportServerDerivedId() throws Exception {
    ProcessResult result =
        exec("project", "create", "web", "--org", ORG, "--description", "Frontend");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("POST /api/v1/organizations/" + ORG + "/projects");
    assertThat(platform.getRequestBodies().get(0))
        .contains("\"name\":\"web\"", "\"description\":\"Frontend\"");
    assertThat(result.stdout()).contains(msg.projectCreated("web", "so-web"));
  }

  @Test
  void shouldCreateProjectWithRepositoryAndClusters() throws Exception {
    Path keyFile = streamxHome.resolve("test-deploy-key");
    Files.writeString(keyFile, "PRIVATE KEY BYTES");
    String expectedKey = java.util.Base64.getEncoder()
        .encodeToString("PRIVATE KEY BYTES".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    ProcessResult result = exec("project", "create", "web", "--org", ORG,
        "--description", "Frontend",
        "--repository-uri", "git@github.com:acme/web.git",
        "--repository-branch", "main",
        "--ssh-private-key", keyFile.toString(),
        "--cluster", "eu-central", "--cluster", "us-east");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("POST /api/v1/organizations/" + ORG + "/projects");
    assertThat(platform.getRequestBodies().get(0))
        .contains("\"name\":\"web\"")
        .contains("\"description\":\"Frontend\"")
        .contains("\"uri\":\"git@github.com:acme/web.git\"")
        .contains("\"branch\":\"main\"")
        .contains("\"sshPrivateKeyBase64\":\"" + expectedKey + "\"")
        .contains("\"clusters\":[\"eu-central\",\"us-east\"]");
  }

  /** The server requires branch whenever a repository is connected; the CLI enforces it early. */
  @Test
  void shouldRequireBranchWhenRepositoryUriIsGiven() throws Exception {
    ProcessResult result = exec("project", "create", "web", "--org", ORG,
        "--repository-uri", "git@github.com:acme/web.git");

    result.assertExitCode(2);
    assertThat(result.stderr()).contains("--repository-branch");
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void shouldFailWhenSshKeyFileIsUnreadable() throws Exception {
    ProcessResult result = exec("project", "create", "web", "--org", ORG,
        "--repository-uri", "git@github.com:acme/web.git",
        "--repository-branch", "main",
        "--ssh-private-key", streamxHome.resolve("nope-key").toString());

    result.assertExitCode(1);
    assertThat(result.stderr()).contains("Could not read SSH private key file");
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void shouldCreateProjectAsJson() throws Exception {
    ProcessResult result = exec("project", "create", "web", "--org", ORG, "--output", "json");

    result.assertSuccess();
    assertThat(result.stdout()).contains("\"id\" : \"so-web\"", "\"name\" : \"web\"");
  }

  /** update is a partial patch: the unspecified field is read back from the current project. */
  @Test
  void shouldUpdateOnlyDescriptionKeepingCurrentName() throws Exception {
    ProcessResult result =
        exec("project", "update", "so-org-web-a1b2c", "--org", ORG, "--description", "New desc");

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
    ProcessResult result = exec("project", "update", "so-org-web-a1b2c", "--org", ORG);

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.projectUpdateNothingToDo());
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void shouldDeleteProjectAfterTypedConfirmation() throws Exception {
    ProcessResult result =
        execWithStdin("so-org-web-a1b2c\n", "project", "delete", "so-org-web-a1b2c", "--org", ORG);

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("DELETE /api/v1/organizations/" + ORG + "/projects/so-org-web-a1b2c");
    assertThat(result.stdout()).contains(msg.projectDeleted("so-org-web-a1b2c"));
  }

  @Test
  void shouldDeleteProjectWithForceWithoutPrompting() throws Exception {
    ProcessResult result = exec("project", "delete", "-f", "so-org-web-a1b2c", "--org", ORG);

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("DELETE /api/v1/organizations/" + ORG + "/projects/so-org-web-a1b2c");
  }

  @Test
  void shouldCancelProjectDeletionWhenConfirmationDoesNotMatch() throws Exception {
    // Typing the ORG id instead of the project id must not confirm.
    ProcessResult result =
        execWithStdin(ORG + "\n", "project", "delete", "so-org-web-a1b2c", "--org", ORG);

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.deleteConfirmMismatch("so-org-web-a1b2c"));
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void shouldShowProjectStatusWithComponents() throws Exception {
    ProcessResult result = exec("project", "status", "so-org-web-a1b2c", "--org", ORG);

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("GET /api/v1/organizations/" + ORG + "/projects/so-org-web-a1b2c/status");
    assertThat(result.stdout()).contains("state = Pending");
    assertThat(result.stdout()).contains("Ready", "Deployed", "web is running");
  }

  @Test
  void shouldListPendingChanges() throws Exception {
    ProcessResult result = exec("project", "pending-changes", "so-org-web-a1b2c", "--org", ORG);

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(
        "GET /api/v1/organizations/" + ORG + "/projects/so-org-web-a1b2c/changes/pending");
    assertThat(result.stdout()).contains("web will be created", "image: nginx", "replicas: 2");
  }

  @Test
  void shouldReportNoPendingChanges() throws Exception {
    platform.failWith(200, "[]");

    ProcessResult result = exec("project", "pending-changes", "so-org-web-a1b2c", "--org", ORG);

    result.assertSuccess();
    assertThat(result.stdout()).contains(msg.projectPendingChangesEmpty());
  }

  /**
   * A crafted project id must stay one path segment. Unencoded, "so-x/status" would route to the
   * status endpoint instead of delete; the raw path is asserted because getPath() decodes it.
   */
  @Test
  void shouldEncodeProjectIdIntoASinglePathSegment() throws Exception {
    exec("project", "delete", "-f", "so-x/status", "--org", ORG);

    assertThat(platform.getRawRequests()).containsExactly(
        "DELETE /api/v1/organizations/" + ORG + "/projects/so-x%2Fstatus");
  }

  @Test
  void projectUseRequiresCurrentOrg() throws Exception {
    ProcessResult result = exec("profile", "project", "use", "so-org-web-a1b2c");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.noCurrentOrg());
  }

  @Test
  void projectUseCurrentLifecycle() throws Exception {
    exec("profile", "org", "use", ORG).assertSuccess();

    ProcessResult use = exec("profile", "project", "use", "so-org-web-a1b2c");
    use.assertSuccess();
    assertThat(use.stdout()).contains(msg.projectUseSet("so-org-web-a1b2c"));

    ProcessResult current = exec("profile", "project", "current");
    current.assertSuccess();
    assertThat(current.stdout().strip()).isEqualTo("so-org-web-a1b2c");
  }

  @Test
  void projectGetFallsBackToFullContext() throws Exception {
    exec("profile", "org", "use", ORG).assertSuccess();
    exec("profile", "project", "use", "so-org-web-a1b2c").assertSuccess();

    ProcessResult result = exec("project", "get");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("GET /api/v1/organizations/" + ORG + "/projects/so-org-web-a1b2c");
  }

  @Test
  void projectGetUsesPositionalProjectWithContextOrg() throws Exception {
    exec("profile", "org", "use", ORG).assertSuccess();

    ProcessResult result = exec("project", "get", "so-org-web-a1b2c");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("GET /api/v1/organizations/" + ORG + "/projects/so-org-web-a1b2c");
  }

  @Test
  void projectListRejectsPositionalArgument() throws Exception {
    ProcessResult result = exec("project", "list", ORG);

    result.assertExitCode(2);
    assertThat(result.stderr()).contains("Unmatched argument");
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void projectCreateRequiresNameArgument() throws Exception {
    ProcessResult result = exec("project", "create", "--org", ORG);

    result.assertExitCode(2);
    assertThat(result.stderr()).contains("<name>");
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void projectListFallsBackToCurrentOrg() throws Exception {
    exec("profile", "org", "use", ORG).assertSuccess();

    ProcessResult result = exec("project", "list");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("GET /api/v1/organizations/" + ORG + "/projects");
  }

  @Test
  void projectGetFailsWithoutProjectContext() throws Exception {
    exec("profile", "org", "use", ORG).assertSuccess();

    ProcessResult result = exec("project", "get");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.noProjectContext());
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void envVarOverridesCurrentProjectFile() throws Exception {
    exec("profile", "org", "use", ORG).assertSuccess();
    exec("profile", "project", "use", "so-org-web-a1b2c").assertSuccess();
    setEnv("STREAMX_PROJECT", "so-org-api-d3e4f");
    try {
      ProcessResult current = exec("profile", "project", "current");
      current.assertSuccess();
      assertThat(current.stdout().strip()).isEqualTo("so-org-api-d3e4f");
    } finally {
      clearEnv("STREAMX_PROJECT");
    }
  }

  @Test
  void completeProjectIdsFallsBackToCurrentOrg() throws Exception {
    exec("profile", "org", "use", ORG).assertSuccess();

    ProcessResult result = exec("__complete-project-ids");

    result.assertSuccess();
    assertThat(result.stdout().strip().lines())
        .containsExactly("so-org-api-d3e4f", "so-org-web-a1b2c");
  }

  @Test
  void unsetProjectKeepsOrg() throws Exception {
    exec("profile", "org", "use", ORG).assertSuccess();
    exec("profile", "project", "use", "so-org-web-a1b2c").assertSuccess();

    ProcessResult result = exec("profile", "project", "unset");

    result.assertSuccess();
    assertThat(result.stdout()).contains(msg.projectUnset());
    assertThat(streamxHome.resolve("profiles/default/current-project")).doesNotExist();
    assertThat(exec("profile", "org", "current").stdout().strip()).isEqualTo(ORG);
    assertThat(exec("profile", "project", "current").exitCode()).isEqualTo(1);
  }

}
