package com.streamx.cli.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.streamx.cli.framework.CliException;
import org.junit.jupiter.api.Test;

class PlatformClientsCleartextTest {

  @Test
  void refusesCleartextRemotePlatformUrl() {
    assertThatThrownBy(() -> new PlatformClients("http://platform.example.com", false, 30_000))
        .isInstanceOf(CliException.class)
        .hasMessageContaining("cleartext HTTP");
  }

  @Test
  void allowsLoopbackHttpForLocalDevelopment() {
    try (PlatformClients clients = new PlatformClients("http://localhost:8085", false, 30_000)) {
      assertThat(clients).isNotNull();
    }
  }
}
