package com.streamx.cli.platform;

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
public interface PlatformConfig {
  String STREAMX_PLATFORM_URL = "streamx.platform.url";
  String STREAMX_PLATFORM_INSECURE = "streamx.platform.insecure";

  @WithName(STREAMX_PLATFORM_URL)
  Optional<String> url();

  @WithName(STREAMX_PLATFORM_INSECURE)
  @WithDefault(BooleanUtils.FALSE)
  boolean insecure();

  static PlatformConfig load() {
    SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
        .withMapping(PlatformConfig.class)
        .addDefaultSources();

    try {
      builder.withSources(new PropertiesConfigSource(StreamxHome.getConfigUrl(), 260));
    } catch (IOException expected) {
    }

    return builder
        .build()
        .getConfigMapping(PlatformConfig.class);
  }
}
