package com.streamx.cli.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.streamx.cli.framework.CliException;
import org.junit.jupiter.api.Test;

class OidcClientCleartextTest {

  @Test
  void refusesCleartextRemoteAuthServer() {
    assertThatThrownBy(() -> new OidcClient("http://idp.example.com/realms/x", "cli", false))
        .isInstanceOf(CliException.class)
        .hasMessageContaining("cleartext HTTP");
  }

  @Test
  void allowsLoopbackHttpForLocalDevelopment() {
    assertThat(new OidcClient("http://127.0.0.1:8080/realms/x", "cli", false)).isNotNull();
  }
}
