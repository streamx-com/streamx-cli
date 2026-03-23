package com.streamx.cli.commands.settings.set;

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
class SetCommandIT extends CliBaseIT {

  @BeforeEach
  void clearConfig() throws IOException {
    Files.deleteIfExists(streamxHome.resolve("application.properties"));
  }

  @Test
  void shouldSetNewProperty() throws Exception {
    ProcessResult result = exec("settings", "set", "a.a.a", "b");

    assertThat(loadProperties().getProperty("a.a.a")).isEqualTo("b");
    assertThat(result.stdout()).isEmpty();
    assertThat(result.stderr()).isEmpty();
    result.assertSuccess();
  }

  @Test
  void shouldUpdateExistingProperty() throws Exception {
    exec("settings", "set", "a.a.a", "b");
    assertThat(loadProperties().getProperty("a.a.a")).isEqualTo("b");

    ProcessResult result = exec("settings", "set", "a.a.a", "c");

    assertThat(loadProperties().getProperty("a.a.a")).isEqualTo("c");
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
