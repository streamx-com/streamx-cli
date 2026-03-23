package com.streamx.cli.ingestion;

import com.streamx.cli.config.StreamxHome;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.Secret;
import io.smallrye.config.SmallRyeConfigBuilder;
import java.io.IOException;
import java.util.Optional;
import picocli.CommandLine;

public class IngestionClientPicocliOptions {

  public static final String HEADING = "%nConnection Options:%n";

  @CommandLine.Option(
      names = {"--ingestion-url", "-u"},
      description = {"StreamX ingestion URL",
          "Falls back to settings property: " + IngestionClientConfig.STREAMX_INGESTION_URL}
  )
  public String url;

  @CommandLine.Option(
      names = {"--auth-token", "-a"},
      description = {"Authentication token",
          "Falls back to settings property: " + IngestionClientConfig.STREAMX_INGESTION_AUTH_TOKEN}
  )
  public String authToken;

  @CommandLine.Option(
      names = {"--insecure", "-k"},
      description = {"Skip TLS verification",
          "Falls back to settings property: " + IngestionClientConfig.STREAMX_INGESTION_INSECURE},
      arity = "0..1",
      fallbackValue = "true",
      defaultValue = "false"
  )
  public Boolean insecure;

  public IngestionClientConfig getIngestionClientConfig() {
    SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
        .withMapping(IngestionClientConfig.class)
        .addDefaultSources();

    try {
      builder.withSources(new PropertiesConfigSource(StreamxHome.getConfigUrl(), 260));
    } catch (IOException e) {
      // If the config file can't be read, continue with other sources
    }

    IngestionClientConfig originalConfig = builder
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
        return Boolean.TRUE.equals(insecure) || originalConfig.insecure();
      }
    };
  }
}
