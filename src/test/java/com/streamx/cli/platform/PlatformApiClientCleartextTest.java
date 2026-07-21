package com.streamx.cli.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.streamx.cli.framework.CliException;
import org.junit.jupiter.api.Test;

class PlatformApiClientCleartextTest {

  @Test
  void refusesCleartextRemotePlatformUrl() {
    assertThatThrownBy(() -> new PlatformApiClient("http://platform.example.com", false))
        .isInstanceOf(CliException.class)
        .hasMessageContaining("cleartext HTTP");
  }

  @Test
  void allowsLoopbackHttpForLocalDevelopment() throws Exception {
    try (PlatformApiClient client = new PlatformApiClient("http://localhost:8085", false)) {
      assertThat(client).isNotNull();
    }
  }
}
