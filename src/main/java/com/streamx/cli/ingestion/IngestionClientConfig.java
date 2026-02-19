package com.streamx.cli.ingestion;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import java.util.Optional;
import org.apache.commons.lang3.BooleanUtils;

@ConfigMapping
public interface IngestionClientConfig {
  String DEFAULT_INGESTION_URL = "http://localhost:8080";
  String STREAMX_INGESTION_URL = "streamx.ingestion.url";
  String STREAMX_INGESTION_AUTH_TOKEN = "streamx.ingestion.auth-token";
  String STREAMX_INGESTION_INSECURE = "streamx.ingestion.insecure";

  @WithName(STREAMX_INGESTION_URL)
  @WithDefault(DEFAULT_INGESTION_URL)
  String url();

  @WithName(STREAMX_INGESTION_AUTH_TOKEN)
  Optional<String> authToken();

  @WithName(STREAMX_INGESTION_INSECURE)
  @WithDefault(BooleanUtils.FALSE)
  boolean insecure();
}
