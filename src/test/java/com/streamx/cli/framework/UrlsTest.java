package com.streamx.cli.framework;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UrlsTest {

  @ParameterizedTest
  @ValueSource(strings = {
      "http://example.com",
      "http://example.com:8080/api",
      "http://10.0.0.5",
      "HTTP://EXAMPLE.COM",
      "http://127.0.0.1.evil.example",
      "http://127.evil.example",
      "http://my_host.example",
      "http://localhost@evil.example/",
      "http://",
      "http:// bad url"
  })
  void cleartextToRemoteOrUnprovableHostsIsBlocked(String url) {
    assertThat(Urls.isCleartextRemote(url)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "https://example.com",
      "https://keycloak.127.0.0.1.nip.io",
      "http://localhost:8085",
      "http://LOCALHOST:8080",
      "http://127.0.0.1:8080",
      "http://127.1.2.3",
      "http://[::1]:8080",
      "not a url",
      "ftp://example.com"
  })
  void httpsAndProvableLoopbackAndOtherSchemesAreAllowed(String url) {
    assertThat(Urls.isCleartextRemote(url)).isFalse();
  }
}
