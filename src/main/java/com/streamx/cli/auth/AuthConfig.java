package com.streamx.cli.auth;

import com.streamx.cli.config.StreamxHome;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import java.io.IOException;
import java.util.Optional;
import org.apache.commons.lang3.BooleanUtils;

@ConfigMapping
public interface AuthConfig {
  String DEFAULT_REALM = "streamx";
  String DEFAULT_CLIENT_ID = "streamx-cli";

  String STREAMX_AUTH_SERVER_URL = "streamx.auth.server-url";
  String STREAMX_AUTH_REALM = "streamx.auth.realm";
  String STREAMX_AUTH_CLIENT_ID = "streamx.auth.client-id";
  String STREAMX_AUTH_INSECURE = "streamx.auth.insecure";

  @WithName(STREAMX_AUTH_SERVER_URL)
  Optional<String> serverUrl();

  @WithName(STREAMX_AUTH_REALM)
  @WithDefault(DEFAULT_REALM)
  String realm();

  @WithName(STREAMX_AUTH_CLIENT_ID)
  @WithDefault(DEFAULT_CLIENT_ID)
  String clientId();

  @WithName(STREAMX_AUTH_INSECURE)
  @WithDefault(BooleanUtils.FALSE)
  boolean insecure();

  static AuthConfig load() {
    SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
        .withMapping(AuthConfig.class)
        .addDefaultSources();

    try {
      builder.withSources(new PropertiesConfigSource(StreamxHome.getConfigUrl(), 260));
    } catch (IOException expected) {
    }

    return builder
        .build()
        .getConfigMapping(AuthConfig.class);
  }
}
