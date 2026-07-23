package com.streamx.cli.commands.info;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.commands.auth.StubOidcServer;
import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class InfoCommandIT extends CliBaseIT {

  private static final ObjectMapper JSON = new ObjectMapper();

  private StubOidcServer oidcServer;

  @BeforeEach
  void cleanProfiles() throws IOException {
    deleteRecursively(streamxHome.resolve("profiles"));
    Files.deleteIfExists(streamxHome.resolve("current-profile"));
  }

  @AfterEach
  void stopServer() {
    if (oidcServer != null) {
      oidcServer.close();
      oidcServer = null;
    }
  }

  @Test
  void worksOnFreshHomeWithoutAnyConfiguration() throws Exception {
    ProcessResult result = exec("info");

    result.assertSuccess();
    assertThat(result.stdout())
        .contains("CLI")
        .contains("active")
        .contains("default")
        .contains("not logged in")
        .contains("(no endpoints configured)")
        .contains("Auth server is not configured");
  }

  @Test
  void probesConfiguredEndpointsAndReportsSources() throws Exception {
    oidcServer = new StubOidcServer("streamx", 0);
    exec("settings", "set", "streamx.auth.server-url", oidcServer.getServerUrl())
        .assertSuccess();
    exec("settings", "set", "streamx.platform.url", oidcServer.getServerUrl())
        .assertSuccess();

    ProcessResult result = exec("info");

    result.assertSuccess();
    assertThat(result.stdout())
        .contains("streamx.auth.server-url")
        .contains("profile")
        .contains("discovery document served")
        .contains("auth");

    ProcessResult json = exec("info", "-o", "json");
    json.assertSuccess();
    JsonNode root = JSON.readTree(json.stdout());
    assertThat(root.path("cli").path("version").asText()).isNotEmpty();
    assertThat(root.path("connectivity").isArray()).isTrue();
    assertThat(root.path("connectivity").toString()).contains("\"UP\"");
  }

  @Test
  void checkFlagFailsWhenAnEndpointIsDown() throws Exception {
    exec("settings", "set", "streamx.platform.url", "https://127.0.0.1:1").assertSuccess();

    ProcessResult result = exec("info", "--check");

    result.assertExitCode(1);
    assertThat(result.stdout()).contains("DOWN");
  }

  @Test
  void reportsStoredLoginAndIssuerMismatch() throws Exception {
    oidcServer = new StubOidcServer("streamx", 0);
    exec("settings", "set", "streamx.auth.server-url", oidcServer.getServerUrl())
        .assertSuccess();

    String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
        "{\"preferred_username\":\"tester\"}".getBytes(StandardCharsets.UTF_8));
    String header = Base64.getUrlEncoder().withoutPadding().encodeToString(
        "{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
    String credentials = """
        {
          "access_token": "%s.%s.sig",
          "refresh_token": "r",
          "expires_at": %d,
          "issuer_url": "https://other-idp.example.com/realms/streamx",
          "client_id": "streamx-cli",
          "insecure": false
        }
        """.formatted(header, payload, Instant.now().plusSeconds(3600).getEpochSecond());
    Files.writeString(
        streamxHome.resolve("profiles/default/config/credentials.json"), credentials);

    ProcessResult result = exec("info");

    result.assertSuccess();
    assertThat(result.stdout())
        .contains("logged in")
        .contains("tester")
        .contains("Stored login belongs to https://other-idp.example.com/realms/streamx");
  }

  private static void deleteRecursively(Path root) throws IOException {
    if (!Files.exists(root)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(root)) {
      paths.sorted(Comparator.reverseOrder()).forEach(path -> {
        try {
          Files.delete(path);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }
}
