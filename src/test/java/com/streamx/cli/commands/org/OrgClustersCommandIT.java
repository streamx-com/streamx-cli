package com.streamx.cli.commands.org;

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
class OrgClustersCommandIT extends CliBaseIT {

  private static final String ORG = "so-testorg";

  private StubPlatformServer platform;

  private Path getCredentialsPath() {
    return streamxHome.resolve("profiles/default/config/credentials.json");
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

  /** processing and edge arrive as separate arrays; the CLI flattens them with a TYPE column. */
  @Test
  void shouldListProcessingAndEdgeClustersTogether() throws Exception {
    ProcessResult result = exec("org", "clusters", "list", ORG);

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("GET /api/v1/organizations/" + ORG + "/clusters");
    assertThat(result.stdout()).contains("ID", "TYPE", "NAME", "ENABLED");
    assertThat(result.stdout()).contains("processing-eu-central", "processing", "EU Central");
    assertThat(result.stdout()).contains("edge-us-east", "edge", "US East");
  }

  @Test
  void shouldListOnlyClusterIdsWhenQuiet() throws Exception {
    ProcessResult result = exec("org", "clusters", "list", ORG, "-q");

    result.assertSuccess();
    assertThat(result.stdout()).isEqualTo("processing-eu-central\nedge-us-east\n");
  }
}
