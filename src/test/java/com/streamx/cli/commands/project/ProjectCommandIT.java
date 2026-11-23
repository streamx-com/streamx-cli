package com.streamx.cli.commands.project;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProjectCommandIT extends CliBaseIT {

  private static final String ORG = "so-testorg";
  private static final String PROJECT_ID = "so-org-web-a1b2c";
  private static final String DEPLOYMENT_PATH =
      "/api/v1/organizations/" + ORG + "/projects/" + PROJECT_ID + "/deployment";

  private StubProjectServer platform;

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

    setEnv(AccessTokens.STREAMX_PLATFORM_TOKEN, "test-access-token");
  }

  @AfterEach
  void tearDown() throws IOException {
    if (platform != null) {
      platform.close();
    }
    clearEnv(AccessTokens.STREAMX_PLATFORM_TOKEN);
    Files.deleteIfExists(streamxHome.resolve("contexts/default/current-org"));
    Files.deleteIfExists(streamxHome.resolve("contexts/default/current-project"));
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
    assertThat(result.stdout()).doesNotContain("CLUSTERS", "REPOSITORY", "SSH KEY");
  }

  @Test
  void shouldListProjectsWideWithClustersAndRepository() throws Exception {
    ProcessResult result = exec("project", "list", "--org", ORG, "--wide");

    result.assertSuccess();
    String api = "/api/v1/organizations/" + ORG + "/projects/so-org-api-d3e4f";
    String web = "/api/v1/organizations/" + ORG + "/projects/so-org-web-a1b2c";
    assertThat(platform.getRequests()).containsExactly(
        "GET /api/v1/organizations/" + ORG + "/projects",
        "GET " + api + "/clusters/enabled", "GET " + api + "/repository",
        "GET " + web + "/clusters/enabled", "GET " + web + "/repository");
    assertThat(result.stdout())
        .contains("CLUSTERS", "REPOSITORY", "SSH KEY")
        .contains("processing-eu-central")
        .contains("git@github.com:acme/web.git")
        .contains("not specified");
  }

  @Test
  void wideListShowsDashesWhenNoClustersOrRepository() throws Exception {
    platform.returnNoClusters();
    platform.returnNoRepository();

    ProcessResult result = exec("project", "list", "--org", ORG, "--wide");

    result.assertSuccess();
    assertThat(result.stdout()).contains("CLUSTERS", "REPOSITORY", "SSH KEY");
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
  void shouldGetProjectWithClustersAndRepository() throws Exception {
    ProcessResult result = exec("project", "get", "so-org-web-a1b2c", "--org", ORG);

    result.assertSuccess();
    String base = "/api/v1/organizations/" + ORG + "/projects/so-org-web-a1b2c";
    assertThat(platform.getRequests())
        .containsExactly("GET " + base, "GET " + base + "/clusters/enabled",
            "GET " + base + "/repository");
    assertThat(result.stdout())
        .contains("id          = so-org-web-a1b2c")
        .contains("clusters    = processing-eu-central")
        .contains("repository  = git@github.com:acme/web.git")
        .contains("branch      = main")
        .contains("ssh key     = not specified");
  }

  @Test
  void shouldGetProjectShowingSshKeySpecifiedWhenPresent() throws Exception {
    platform.sshKeyExists(true);

    ProcessResult result = exec("project", "get", "so-org-web-a1b2c", "--org", ORG);

    result.assertSuccess();
    assertThat(result.stdout()).contains("ssh key     = specified");
  }

  @Test
  void shouldGetProjectShowingNotConnectedWhenNoRepository() throws Exception {
    platform.returnNoRepository();

    ProcessResult result = exec("project", "get", "so-org-web-a1b2c", "--org", ORG);

    result.assertSuccess();
    assertThat(result.stdout())
        .contains("repository  = not connected")
        .contains("branch      = -")
        .contains("ssh key     = not specified");
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
        "--clusters", "eu-central,us-east");

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
    assertThat(result.stderr()).contains(msg.projectDeleted("so-org-web-a1b2c"));
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
        .containsExactly("GET /api/v1/organizations/" + ORG
            + "/projects/so-org-web-a1b2c/deployment/status");
    assertThat(result.stdout()).contains("state = Pending");
    assertThat(result.stdout()).contains("Ready", "Deployed", "web is running");
  }

  @Test
  void shouldShowLiveAndDesiredDeploymentConfiguration() throws Exception {
    platform.failWith(200, """
        {"generation":7,"domains":["example.com"]}
        """);

    ProcessResult live = deployment("live", "-o", "json");
    live.assertSuccess();
    assertThat(live.stdout()).contains("\"generation\"", "7", "example.com");

    ProcessResult desired = deployment("desired", "-o", "yaml");
    desired.assertSuccess();
    assertThat(desired.stdout()).contains("generation: 7", "example.com");
    assertThat(platform.getRequests()).containsExactly(
        "GET " + DEPLOYMENT_PATH + "/live", "GET " + DEPLOYMENT_PATH + "/desired");
  }

  @Test
  void shouldUseCurrentProjectForDeploymentCommands() throws Exception {
    exec("context", "org", "use", ORG).assertSuccess();
    exec("context", "project", "use", PROJECT_ID).assertSuccess();
    platform.failWith(200, """
        {"generation":7}
        """);

    exec("project", "deployment", "desired").assertSuccess();
    assertThat(platform.getRequests()).last().isEqualTo("GET " + DEPLOYMENT_PATH + "/desired");
  }

  @Test
  void shouldShowDeploymentDiffAndSendPathAsJsonPointer() throws Exception {
    platform.failWith(200, """
        {"present":true,"staged":{"domains":["new.example.com"]},"differences":[
          {"operation":"replace","path":"/domains","before":["old.example.com"],
           "after":["new.example.com"]}
        ]}
        """);

    ProcessResult diff = deployment("diff");
    diff.assertSuccess();
    assertThat(diff.stdout()).contains("replace", "/domains",
        "[\"old.example.com\"]", "[\"new.example.com\"]");

    ProcessResult filtered = deployment("diff", "--path", "/domains", "-o", "json");
    filtered.assertSuccess();
    assertThat(filtered.stdout()).contains(
        "\"present\"", "true", "\"staged\"", "new.example.com", "\"differences\"");
    assertThat(platform.getRequests()).containsExactly(
        "GET " + DEPLOYMENT_PATH + "/diff", "POST " + DEPLOYMENT_PATH + "/diff/path");
    assertThat(platform.getRequestBodies().get(1)).isEqualTo("{\"path\":\"/domains\"}");
  }

  @Test
  void shouldDistinguishEmptyDiffFromAbsentLocalChanges() throws Exception {
    platform.failWith(200, """
        {"present":true,"staged":{},"differences":[]}
        """);
    ProcessResult empty = deployment("diff");
    empty.assertSuccess();
    assertThat(empty.stdout()).contains(msg.projectDeploymentEmptyDiff());

    platform.failWith(200, """
        {"present":false,"staged":{},"differences":[]}
        """);
    ProcessResult absent = deployment("diff");
    absent.assertSuccess();
    assertThat(absent.stdout()).contains(msg.projectDeploymentNoLocalChanges());
  }

  @Test
  void shouldDiscardAndPushDeploymentChanges() throws Exception {
    platform.failWith(204, "");
    deployment("discard").assertSuccess();
    ProcessResult noPush = deployment("push");
    noPush.assertSuccess();
    assertThat(noPush.stdout()).contains(msg.projectDeploymentNothingToPush());

    platform.failWith(200, """
        {"generation":8}
        """);
    ProcessResult push = deployment("push");
    push.assertSuccess();
    assertThat(push.stdout()).contains(msg.projectDeploymentPushed(8));
    assertThat(platform.getRequests()).containsExactly(
        "DELETE " + DEPLOYMENT_PATH + "/diff",
        "POST " + DEPLOYMENT_PATH + "/push",
        "POST " + DEPLOYMENT_PATH + "/push");
  }

  private ProcessResult deployment(String command, String... options) throws Exception {
    var args = new ArrayList<>(
        List.of("project", "deployment", command, "--org", ORG, "--project", PROJECT_ID));
    args.addAll(List.of(options));
    return exec(args.toArray(String[]::new));
  }

  /**
   * A project id containing slashes must stay one path segment.
   * The raw path is asserted because getPath() decodes it.
   */
  @Test
  void shouldEncodeProjectIdIntoASinglePathSegment() throws Exception {
    exec("project", "delete", "-f", "so-x/deployment/status", "--org", ORG);

    assertThat(platform.getRawRequests()).containsExactly(
        "DELETE /api/v1/organizations/" + ORG + "/projects/so-x%2Fdeployment%2Fstatus");
  }

  @Test
  void projectUseRequiresCurrentOrg() throws Exception {
    ProcessResult result = exec("context", "project", "use", "so-org-web-a1b2c");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.noCurrentOrg());
  }

  @Test
  void projectUseStoresTheProjectAndProjectCurrentPrintsItBack() throws Exception {
    exec("context", "org", "use", ORG).assertSuccess();

    ProcessResult use = exec("context", "project", "use", "so-org-web-a1b2c");
    use.assertSuccess();
    assertThat(use.stderr()).contains(msg.projectUseSet("so-org-web-a1b2c"));

    ProcessResult current = exec("context", "project", "current");
    current.assertSuccess();
    assertThat(current.stdout().strip()).isEqualTo("so-org-web-a1b2c");
  }

  @Test
  void projectGetFallsBackToFullContext() throws Exception {
    exec("context", "org", "use", ORG).assertSuccess();
    exec("context", "project", "use", "so-org-web-a1b2c").assertSuccess();

    ProcessResult result = exec("project", "get");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .first().isEqualTo("GET /api/v1/organizations/" + ORG + "/projects/so-org-web-a1b2c");
  }

  @Test
  void projectGetUsesPositionalProjectWithContextOrg() throws Exception {
    exec("context", "org", "use", ORG).assertSuccess();

    ProcessResult result = exec("project", "get", "so-org-web-a1b2c");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .first().isEqualTo("GET /api/v1/organizations/" + ORG + "/projects/so-org-web-a1b2c");
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
    exec("context", "org", "use", ORG).assertSuccess();

    ProcessResult result = exec("project", "list");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("GET /api/v1/organizations/" + ORG + "/projects");
  }

  @Test
  void projectGetFailsWithoutProjectContext() throws Exception {
    exec("context", "org", "use", ORG).assertSuccess();

    ProcessResult result = exec("project", "get");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.noProjectContext());
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void envVarOverridesCurrentProjectFile() throws Exception {
    exec("context", "org", "use", ORG).assertSuccess();
    exec("context", "project", "use", "so-org-web-a1b2c").assertSuccess();
    setEnv("STREAMX_PROJECT", "so-org-api-d3e4f");
    try {
      ProcessResult current = exec("context", "project", "current");
      current.assertSuccess();
      assertThat(current.stdout().strip()).isEqualTo("so-org-api-d3e4f");
    } finally {
      clearEnv("STREAMX_PROJECT");
    }
  }

  @Test
  void completeProjectIdsFallsBackToCurrentOrg() throws Exception {
    exec("context", "org", "use", ORG).assertSuccess();

    ProcessResult result = exec("__complete-project-ids");

    result.assertSuccess();
    assertThat(result.stdout().strip().lines())
        .containsExactly("so-org-api-d3e4f", "so-org-web-a1b2c");
  }

  @Test
  void unsetProjectKeepsOrg() throws Exception {
    exec("context", "org", "use", ORG).assertSuccess();
    exec("context", "project", "use", "so-org-web-a1b2c").assertSuccess();

    ProcessResult result = exec("context", "project", "unset");

    result.assertSuccess();
    assertThat(result.stderr()).contains(msg.projectUnset());
    assertThat(streamxHome.resolve("contexts/default/current-project")).doesNotExist();
    assertThat(exec("context", "org", "current").stdout().strip()).isEqualTo(ORG);
    assertThat(exec("context", "project", "current").exitCode()).isEqualTo(1);
  }

}
