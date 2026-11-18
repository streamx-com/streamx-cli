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
    exec("context", "org", "use", ORG).assertSuccess();
    exec("context", "project", "use", PROJECT).assertSuccess();

    ProcessResult result = exec("project", "clusters", "list", "-q");

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly("GET " + CLUSTERS_PATH);
    assertThat(result.stdout()).isEqualTo("processing-eu-central\nedge-us-east\n");
  }

  @Test
  void shouldSetProjectClustersAfterValidatingAgainstAvailable() throws Exception {
    ProcessResult result = exec("project", "clusters", "set",
        "processing-eu-central,edge-us-east", "--org", ORG, "--project", PROJECT);

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(
        "GET " + CLUSTERS_PATH,
        "PATCH " + CLUSTERS_PATH);
    assertThat(platform.getRequestBodies().get(1))
        .isEqualTo("[\"processing-eu-central\",\"edge-us-east\"]");
    assertThat(result.stderr()).contains(
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
  void shouldStageAnEmptyClusterListWithClear() throws Exception {
    ProcessResult result =
        exec("project", "clusters", "set", "--clear", "--org", ORG, "--project", PROJECT);

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly("PATCH " + CLUSTERS_PATH);
    assertThat(platform.getRequestBodies()).containsExactly("[]");
    assertThat(result.stderr()).contains(msg.projectClustersCleared(PROJECT));
  }

  @Test
  void shouldRejectClearWithClusterIds() throws Exception {
    ProcessResult result = exec("project", "clusters", "set", "--clear",
        "edge-us-east", "--org", ORG, "--project", PROJECT);

    result.assertExitCode(2);
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void shouldShowProjectSelectorsInEnableAndDisableHelp() throws Exception {
    for (String command : new String[]{"enable", "disable"}) {
      ProcessResult help = exec("project", "clusters", command, "--help");
      assertThat(help.stdout()).contains("--org=<orgId>", "--project=<projectId>", "<clusterId>");
    }
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void shouldStageTwoClusterChangesWithSeparatePatches() throws Exception {
    ProcessResult enable = exec("project", "clusters", "enable",
        "edge-us-east", "--org", ORG, "--project", PROJECT);
    ProcessResult disable = exec("project", "clusters", "disable",
        "processing-eu-central", "--org", ORG, "--project", PROJECT);

    enable.assertSuccess();
    disable.assertSuccess();
    assertThat(enable.stderr()).contains(msg.projectClusterEnabled("edge-us-east", PROJECT));
    assertThat(disable.stderr())
        .contains(msg.projectClusterDisabled("processing-eu-central", PROJECT));
    assertThat(platform.getRequests()).containsExactly(
        "PATCH " + CLUSTERS_PATH + "/edge-us-east",
        "PATCH " + CLUSTERS_PATH + "/processing-eu-central");
    assertThat(platform.getRequestBodies()).containsExactly("true", "false");

    ProcessResult live = exec("project", "clusters", "list", "--org", ORG, "--project", PROJECT);
    live.assertSuccess();
    assertThat(live.stdout()).containsPattern("processing-eu-central[^\\n]*true")
        .containsPattern("edge-us-east[^\\n]*false");
  }

  @Test
  void shouldReportUnknownClusterRejectedByPlatform() throws Exception {
    ProcessResult result = exec("project", "clusters", "enable",
        "nope-cluster", "--org", ORG, "--project", PROJECT);

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(
        msg.projectClusterUnknown("nope-cluster", "edge-us-east, processing-eu-central"));
    assertThat(platform.getRequests()).containsExactly("PATCH " + CLUSTERS_PATH + "/nope-cluster");
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
    exec("context", "org", "use", ORG).assertSuccess();

    ProcessResult result = exec("__complete-cluster-ids");

    result.assertSuccess();
    assertThat(result.stdout().strip().lines())
        .containsExactly("edge-us-east", "processing-eu-central");
  }
}
