package com.streamx.cli.commands.settings.unset;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UnsetCommandIT extends CliBaseIT {

  @BeforeEach
  void clearConfig() throws IOException {
    Files.deleteIfExists(streamxHome.resolve("application.properties"));
  }

  @Test
  void shouldUnsetExistingProperty() throws Exception {
    exec("settings", "set", "a.a.a", "b");
    assertThat(loadProperties().getProperty("a.a.a")).isEqualTo("b");

    ProcessResult result = exec("settings", "unset", "a.a.a");

    assertThat(loadProperties().getProperty("a.a.a")).isNull();
    assertThat(result.stdout()).isEmpty();
    assertThat(result.stderr()).isEmpty();
    result.assertSuccess();
  }

  @Test
  void shouldSucceedWhenUnsettingNonExistentProperty() throws Exception {
    ProcessResult result = exec("settings", "unset", "non.existent.key");

    assertThat(loadProperties().getProperty("non.existent.key")).isNull();
    assertThat(result.stdout()).isEmpty();
    assertThat(result.stderr()).isEmpty();
    result.assertSuccess();
  }

  @Test
  void shouldNotAffectOtherProperties() throws Exception {
    exec("settings", "set", "a.a.a", "b");
    exec("settings", "set", "c.c.c", "d");

    ProcessResult result = exec("settings", "unset", "a.a.a");

    assertThat(loadProperties().getProperty("a.a.a")).isNull();
    assertThat(loadProperties().getProperty("c.c.c")).isEqualTo("d");
    assertThat(result.stdout()).isEmpty();
    assertThat(result.stderr()).isEmpty();
    result.assertSuccess();
  }

  private Properties loadProperties() throws IOException {
    Properties props = new Properties();
    Path configFile = streamxHome.resolve("application.properties");
    try (InputStream inputStream = Files.newInputStream(configFile)) {
      props.load(inputStream);
    }
    return props;
  }
}
