package com.streamx.cli.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@QuarkusTest
class IngestionClientPicocliOptionsIT extends CliBaseIT {

  @BeforeEach
  void clearConfig() throws IOException {
    Files.deleteIfExists(streamxHome.resolve("application.properties"));
  }

  @Nested
  class ConfigFallbackTests {

    @Test
    void shouldUseIngestionUrlFromConfig() throws Exception {
      exec("settings", "set",
          IngestionClientConfig.STREAMX_INGESTION_URL, "http://localhost:19999");

      ProcessResult result = exec("publish", "stream", "--verbose");

      assertThat(result.stderr())
          .contains(IngestionClientConfig.STREAMX_INGESTION_URL + " = http://localhost:19999");
    }

    @Test
    void shouldUseAuthTokenFromConfig() throws Exception {
      exec("settings", "set",
          IngestionClientConfig.STREAMX_INGESTION_AUTH_TOKEN, "my-secret-token");

      ProcessResult result = exec("publish", "stream", "--verbose");

      assertThat(result.stderr())
          .contains(IngestionClientConfig.STREAMX_INGESTION_AUTH_TOKEN + " = *****");
    }

    @Test
    void shouldUseInsecureFromConfig() throws Exception {
      exec("settings", "set", IngestionClientConfig.STREAMX_INGESTION_INSECURE, "true");

      ProcessResult result = exec("publish", "stream", "--verbose");

      assertThat(result.stderr())
          .contains(IngestionClientConfig.STREAMX_INGESTION_INSECURE + " = true");
    }

    @Test
    void shouldFallBackToAllConfigValues() throws Exception {
      exec("settings", "set",
          IngestionClientConfig.STREAMX_INGESTION_URL, "http://config-url:8080");
      exec("settings", "set",
          IngestionClientConfig.STREAMX_INGESTION_AUTH_TOKEN, "config-token");
      exec("settings", "set", IngestionClientConfig.STREAMX_INGESTION_INSECURE, "true");

      ProcessResult result = exec("publish", "stream", "--verbose");

      assertThat(result.stderr())
          .contains(IngestionClientConfig.STREAMX_INGESTION_URL + " = http://config-url:8080")
          .contains(IngestionClientConfig.STREAMX_INGESTION_AUTH_TOKEN + " = *****")
          .contains(IngestionClientConfig.STREAMX_INGESTION_INSECURE + " = true");
    }
  }

  @Nested
  class CliFlagOverrideTests {

    @Test
    void shouldOverrideConfigUrlWithCliFlag() throws Exception {
      exec("settings", "set",
          IngestionClientConfig.STREAMX_INGESTION_URL, "http://config-url:9999");

      ProcessResult result = exec("publish", "stream", "--verbose",
          "--ingestion-url", "http://cli-url:8888");

      assertThat(result.stderr())
          .contains(IngestionClientConfig.STREAMX_INGESTION_URL + " = http://cli-url:8888");
      assertThat(result.stderr()).doesNotContain("http://config-url:9999");
    }

    @Test
    void shouldOverrideConfigAuthTokenWithCliFlag() throws Exception {
      exec("settings", "set",
          IngestionClientConfig.STREAMX_INGESTION_AUTH_TOKEN, "config-token");

      ProcessResult result = exec("publish", "stream", "--verbose",
          "--auth-token", "cli-token");

      assertThat(result.stderr())
          .contains(IngestionClientConfig.STREAMX_INGESTION_AUTH_TOKEN + " = *****");
    }

    @Test
    void shouldOverrideConfigInsecureWithCliFlag() throws Exception {
      exec("settings", "set", IngestionClientConfig.STREAMX_INGESTION_INSECURE, "false");

      ProcessResult result = exec("publish", "stream", "--verbose",
          "--insecure");

      assertThat(result.stderr())
          .contains(IngestionClientConfig.STREAMX_INGESTION_INSECURE + " = true");
    }

    @Test
    void shouldOverrideAllConfigValuesWithCliFlags() throws Exception {
      exec("settings", "set",
          IngestionClientConfig.STREAMX_INGESTION_URL, "http://config-url:9999");
      exec("settings", "set",
          IngestionClientConfig.STREAMX_INGESTION_AUTH_TOKEN, "config-token");
      exec("settings", "set", IngestionClientConfig.STREAMX_INGESTION_INSECURE, "false");

      ProcessResult result = exec("publish", "stream", "--verbose",
          "--ingestion-url", "http://cli-url:8888",
          "--auth-token", "cli-token",
          "--insecure");

      assertThat(result.stderr())
          .contains(IngestionClientConfig.STREAMX_INGESTION_URL + " = http://cli-url:8888")
          .contains(IngestionClientConfig.STREAMX_INGESTION_AUTH_TOKEN + " = *****")
          .contains(IngestionClientConfig.STREAMX_INGESTION_INSECURE + " = true");
      assertThat(result.stderr()).doesNotContain("http://config-url:9999");
    }
  }

  @Nested
  class VerboseOutputTests {

    @Test
    void shouldContainAllConfigKeys() throws Exception {
      ProcessResult result = exec("publish", "stream", "--verbose");

      assertThat(result.stderr())
          .contains(IngestionClientConfig.STREAMX_INGESTION_URL)
          .contains(IngestionClientConfig.STREAMX_INGESTION_AUTH_TOKEN)
          .contains(IngestionClientConfig.STREAMX_INGESTION_INSECURE);
    }

    @Test
    void shouldMaskAuthTokenInOutput() throws Exception {
      ProcessResult result = exec("publish", "stream", "--verbose",
          "--ingestion-url", "http://test:8080",
          "--auth-token", "my-secret-token",
          "--insecure");

      assertThat(result.stderr())
          .contains(IngestionClientConfig.STREAMX_INGESTION_URL + " = http://test:8080")
          .contains(IngestionClientConfig.STREAMX_INGESTION_INSECURE + " = true")
          .contains(IngestionClientConfig.STREAMX_INGESTION_AUTH_TOKEN + " = *****")
          .doesNotContain("my-secret-token");
    }
  }
}
