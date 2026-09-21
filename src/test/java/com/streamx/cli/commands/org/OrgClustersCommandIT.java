package com.streamx.cli.commands.org;

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
class OrgClustersCommandIT extends CliBaseIT {

  private static final String ORG = "so-testorg";

  private StubPlatformServer platform;

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

    setEnv(AccessTokens.STREAMX_PLATFORM_TOKEN, "test-access-token");
  }

  @AfterEach
  void tearDown() {
    if (platform != null) {
      platform.close();
    }
    clearEnv(AccessTokens.STREAMX_PLATFORM_TOKEN);
  }

  /** processing and edge arrive as separate arrays; the CLI flattens them with a TYPE column. */
  @Test
  void shouldListProcessingAndEdgeClustersTogether() throws Exception {
    ProcessResult result = exec("org", "clusters", "list", "--org", ORG);

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("GET /api/v1/organizations/" + ORG + "/clusters");
    assertThat(result.stdout()).contains("ID", "TYPE", "NAME", "ENABLED");
    assertThat(result.stdout()).contains("processing-eu-central", "processing", "EU Central");
    assertThat(result.stdout()).contains("edge-us-east", "edge", "US East");
  }

  @Test
  void shouldListOnlyClusterIdsWhenQuiet() throws Exception {
    ProcessResult result = exec("org", "clusters", "list", "--org", ORG, "-q");

    result.assertSuccess();
    assertThat(result.stdout()).isEqualTo("processing-eu-central\nedge-us-east\n");
  }
}
