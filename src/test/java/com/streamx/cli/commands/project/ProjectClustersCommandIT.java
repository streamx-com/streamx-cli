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
class ProjectClustersCommandIT extends CliBaseIT {

  private static final String ORG = "so-testorg";
  private static final String PROJECT = "so-org-web-a1b2c";
  private static final String CLUSTERS_PATH =
      "/api/v1/organizations/" + ORG + "/projects/" + PROJECT + "/clusters";

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
  void shouldListProjectClustersWithEnabledColumn() throws Exception {
    ProcessResult result =
        exec("project", "clusters", "list", "--org", ORG, "--project", PROJECT);

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly("GET " + CLUSTERS_PATH);
    assertThat(result.stdout()).contains("ID", "TYPE", "NAME", "ENABLED");
    assertThat(result.stdout()).contains("processing-eu-central", "true");
    assertThat(result.stdout()).contains("edge-us-east", "false");
  }

  @Test
  void shouldListProjectClustersFromContext() throws Exception {
    exec("profile", "org", "use", ORG).assertSuccess();
    exec("profile", "project", "use", PROJECT).assertSuccess();

    ProcessResult result = exec("project", "clusters", "list", "-q");

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly("GET " + CLUSTERS_PATH);
    assertThat(result.stdout()).isEqualTo("processing-eu-central\nedge-us-east\n");
  }

  @Test
  void shouldSetProjectClustersAfterValidatingAgainstAvailable() throws Exception {
    ProcessResult result = exec("project", "clusters", "set",
        "processing-eu-central", "edge-us-east", "--org", ORG, "--project", PROJECT);

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(
        "GET " + CLUSTERS_PATH,
        "PATCH " + CLUSTERS_PATH);
    assertThat(platform.getRequestBodies().get(1))
        .isEqualTo("[\"processing-eu-central\",\"edge-us-east\"]");
    assertThat(result.stdout()).contains(
        msg.projectClustersSet(PROJECT, "processing-eu-central, edge-us-east"));
  }

  @Test
  void shouldRejectSettingUnknownCluster() throws Exception {
    ProcessResult result = exec("project", "clusters", "set",
        "nope-cluster", "--org", ORG, "--project", PROJECT);

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(
        msg.projectClusterUnknown("nope-cluster", "edge-us-east, processing-eu-central"));
    assertThat(platform.getRequests()).containsExactly("GET " + CLUSTERS_PATH);
  }

  @Test
  void shouldRequireAtLeastOneClusterForSet() throws Exception {
    ProcessResult result =
        exec("project", "clusters", "set", "--org", ORG, "--project", PROJECT);

    result.assertExitCode(2);
    assertThat(result.stderr()).contains("<clusterId>");
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void shouldEnableDisabledCluster() throws Exception {
    ProcessResult result = exec("project", "clusters", "enable",
        "edge-us-east", "--org", ORG, "--project", PROJECT);

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(
        "GET " + CLUSTERS_PATH,
        "PATCH " + CLUSTERS_PATH);
    assertThat(platform.getRequestBodies().get(1))
        .isEqualTo("[\"processing-eu-central\",\"edge-us-east\"]");
    assertThat(result.stdout()).contains(msg.projectClusterEnabled("edge-us-east", PROJECT));
  }

  @Test
  void shouldNotPatchWhenClusterAlreadyEnabled() throws Exception {
    ProcessResult result = exec("project", "clusters", "enable",
        "processing-eu-central", "--org", ORG, "--project", PROJECT);

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly("GET " + CLUSTERS_PATH);
    assertThat(result.stdout())
        .contains(msg.projectClusterAlreadyEnabled("processing-eu-central", PROJECT));
  }

  @Test
  void shouldDisableEnabledCluster() throws Exception {
    ProcessResult result = exec("project", "clusters", "disable",
        "processing-eu-central", "--org", ORG, "--project", PROJECT);

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(
        "GET " + CLUSTERS_PATH,
        "PATCH " + CLUSTERS_PATH);
    assertThat(platform.getRequestBodies().get(1)).isEqualTo("[]");
    assertThat(result.stdout())
        .contains(msg.projectClusterDisabled("processing-eu-central", PROJECT));
  }

  @Test
  void shouldNotPatchWhenClusterAlreadyDisabled() throws Exception {
    ProcessResult result = exec("project", "clusters", "disable",
        "edge-us-east", "--org", ORG, "--project", PROJECT);

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly("GET " + CLUSTERS_PATH);
    assertThat(result.stdout())
        .contains(msg.projectClusterAlreadyDisabled("edge-us-east", PROJECT));
  }

  @Test
  void shouldRejectEnablingUnknownCluster() throws Exception {
    ProcessResult result = exec("project", "clusters", "enable",
        "nope-cluster", "--org", ORG, "--project", PROJECT);

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(
        msg.projectClusterUnknown("nope-cluster", "edge-us-east, processing-eu-central"));
    assertThat(platform.getRequests()).containsExactly("GET " + CLUSTERS_PATH);
  }

  @Test
  void completeClusterIdsListsOrgClusters() throws Exception {
    ProcessResult result = exec("__complete-cluster-ids", ORG);

    result.assertSuccess();
    assertThat(result.stdout().strip().lines())
        .containsExactly("edge-us-east", "processing-eu-central");
  }

  @Test
  void completeClusterIdsFallsBackToCurrentOrg() throws Exception {
    exec("profile", "org", "use", ORG).assertSuccess();

    ProcessResult result = exec("__complete-cluster-ids");

    result.assertSuccess();
    assertThat(result.stdout().strip().lines())
        .containsExactly("edge-us-east", "processing-eu-central");
  }
}
