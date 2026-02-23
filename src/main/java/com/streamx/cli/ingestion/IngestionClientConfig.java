package com.streamx.cli.ingestion;

import static com.streamx.cli.i18n.MessageProvider.msg;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.Secret;
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
  Optional<Secret<String>> authToken();

  @WithName(STREAMX_INGESTION_INSECURE)
  @WithDefault(BooleanUtils.FALSE)
  boolean insecure();

  public static String prettyPrint(IngestionClientConfig config) {
    return """
        %s = %s
        %s = %s
        %s = %s
        """.formatted(
        STREAMX_INGESTION_URL, config.url(),
        STREAMX_INGESTION_AUTH_TOKEN, config.authToken()
            .map(s -> msg.ingestionTokenMasked()).orElse(msg.ingestionTokenNotSet()),
        STREAMX_INGESTION_INSECURE, config.insecure()
    );
  }
}