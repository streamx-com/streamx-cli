package com.streamx.cli.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class IngestionClientPicocliOptionsTest {

  @AfterEach
  void afterEach() {
    System.clearProperty(IngestionClientConfig.STREAMX_INGESTION_URL);
    System.clearProperty(IngestionClientConfig.STREAMX_INGESTION_AUTH_TOKEN);
    System.clearProperty(IngestionClientConfig.STREAMX_INGESTION_INSECURE);
  }

  @Nested
  class MergedConfigTests {

    @Test
    void shouldOverrideAllConfigValuesWithCliOptions() {
      System.setProperty(IngestionClientConfig.STREAMX_INGESTION_URL, "http://config-url:8080");
      System.setProperty(IngestionClientConfig.STREAMX_INGESTION_AUTH_TOKEN, "config-token");
      System.setProperty(IngestionClientConfig.STREAMX_INGESTION_INSECURE, "false");

      var options = new IngestionClientPicocliOptions();
      options.url = "http://cli-url:9090";
      options.authToken = "cli-token";
      options.insecure = true;

      IngestionClientConfig config = options.getIngestionClientConfig();

      assertThat(config.url()).isEqualTo("http://cli-url:9090");
      assertThat(config.authToken().get().get()).isEqualTo("cli-token");
      assertThat(config.insecure()).isTrue();
    }

    @Test
    void shouldFallBackToAllConfigValuesWhenCliOptionsAreNull() {
      System.setProperty(IngestionClientConfig.STREAMX_INGESTION_URL, "http://config-url:8080");
      System.setProperty(IngestionClientConfig.STREAMX_INGESTION_AUTH_TOKEN, "config-token");
      System.setProperty(IngestionClientConfig.STREAMX_INGESTION_INSECURE, "true");

      var options = new IngestionClientPicocliOptions();
      options.url = null;
      options.authToken = null;
      options.insecure = null;

      IngestionClientConfig config = options.getIngestionClientConfig();

      assertThat(config.url()).isEqualTo("http://config-url:8080");
      assertThat(config.authToken().get().get()).isEqualTo("config-token");
      assertThat(config.insecure()).isTrue();
    }
  }

  @Nested
  class PrettyPrintTests {

    @Test
    void shouldMaskAuthTokenInOutput() {
      var options = new IngestionClientPicocliOptions();
      options.url = "http://test:8080";
      options.authToken = "my-secret-token";
      options.insecure = true;

      IngestionClientConfig config = options.getIngestionClientConfig();
      String output = IngestionClientConfig.prettyPrint(config);

      assertThat(output)
          .contains(IngestionClientConfig.STREAMX_INGESTION_URL + " = http://test:8080")
          .contains(IngestionClientConfig.STREAMX_INGESTION_INSECURE + " = true")
          .doesNotContain("my-secret-token");
    }

    @Test
    void shouldContainAllConfigKeys() {
      var options = new IngestionClientPicocliOptions();
      options.url = "http://localhost:8080";
      options.insecure = false;

      IngestionClientConfig config = options.getIngestionClientConfig();
      String output = IngestionClientConfig.prettyPrint(config);

      assertThat(output)
          .contains(IngestionClientConfig.STREAMX_INGESTION_URL)
          .contains(IngestionClientConfig.STREAMX_INGESTION_AUTH_TOKEN)
          .contains(IngestionClientConfig.STREAMX_INGESTION_INSECURE);
    }
  }
}