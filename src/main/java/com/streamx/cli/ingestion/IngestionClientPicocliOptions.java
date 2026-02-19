package com.streamx.cli.ingestion;

import io.smallrye.config.SmallRyeConfigBuilder;
import picocli.CommandLine;

import java.util.Optional;

public class IngestionClientPicocliOptions {

  @CommandLine.Option(
      names = {"--ingestion-url"},
      description = "StreamX ingestion URL (default: ${DEFAULT-VALUE})",
      defaultValue = "${streamx.ingestion.url}",
      fallbackValue = CommandLine.Parameters.NULL_VALUE
  )
  public String url;

  @CommandLine.Option(
      names = {"--auth-token"},
      description = "Authentication token"
  )
  public String authToken;

  @CommandLine.Option(
      names = {"--insecure"},
      description = "Skip TLS verification"
  )
  public Boolean insecure;

  public IngestionClientConfig getIngestionClientConfig() {
    IngestionClientConfig originalConfig = new SmallRyeConfigBuilder()
        .withMapping(IngestionClientConfig.class)
        .addDefaultSources()
        .build()
        .getConfigMapping(IngestionClientConfig.class);

    // Merge options with original config
    return new IngestionClientConfig() {
      @Override
      public String url() {
        return url == null ? originalConfig.url() : url;
      }

      @Override
      public Optional<String> authToken() {
        return authToken == null ? originalConfig.authToken() : Optional.of(authToken);
      }

      @Override
      public boolean insecure() {
        return insecure == null ? originalConfig.insecure() : insecure;
      }
    };
  }
}