package com.streamx.cli.ingestion;

import io.smallrye.config.Secret;
import io.smallrye.config.SmallRyeConfigBuilder;
import java.util.Optional;
import picocli.CommandLine;

public class IngestionClientPicocliOptions {

  @CommandLine.Option(
      names = {"--ingestion-url", "-u"},
      description = "StreamX ingestion URL"
  )
  public String url;

  @CommandLine.Option(
      names = {"--auth-token", "-a"},
      description = "Authentication token"
  )
  public String authToken;

  @CommandLine.Option(
      names = {"--insecure", "-k"},
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
        return (url == null || url.isEmpty()) ? originalConfig.url() : url;
      }

      @Override
      public Optional<Secret<String>> authToken() {
        return authToken == null
            ? originalConfig.authToken()
            : Optional.of((Secret<String>) () -> authToken);
      }

      @Override
      public boolean insecure() {
        return insecure == null ? originalConfig.insecure() : insecure;
      }
    };
  }
}