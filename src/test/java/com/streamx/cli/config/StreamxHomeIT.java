package com.streamx.cli.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.test.CliBaseIT;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StreamxHomeIT extends CliBaseIT {

  @Test
  void shouldUseStreamxHomeCliArgOverDefault(
      @TempDir Path customHome
  ) throws Exception {
    exec("settings", "set", "--streamx-home", customHome.toString(),
        "my.key", "my.value");

    Path configFile = customHome.resolve(CONFIG_FILE_PATH);
    assertThat(configFile).exists();
    assertThat(Files.readString(configFile)).contains("my.key=my.value");
  }

  @Test
  void shouldUseStreamxHomeCliArgOverEnv(@TempDir Path customHome) throws Exception {
    exec("settings", "set", "--streamx-home", customHome.toString(),
        "custom.key", "custom.value");

    Path customConfig = customHome.resolve(CONFIG_FILE_PATH);
    assertThat(customConfig).exists();
    assertThat(Files.readString(customConfig)).contains("custom.key=custom.value");

    Path defaultConfig = getConfigPath();
    if (Files.exists(defaultConfig)) {
      assertThat(Files.readString(defaultConfig)).doesNotContain("custom.key");
    }
  }

  @Test
  void shouldUseShortAlias(@TempDir Path customHome) throws Exception {
    exec("settings", "set", "-H", customHome.toString(), "alias.key", "alias.value");

    Path configFile = customHome.resolve(CONFIG_FILE_PATH);
    assertThat(configFile).exists();
    assertThat(Files.readString(configFile)).contains("alias.key=alias.value");
  }

  @Test
  void shouldReadFromCustomHome(@TempDir Path customHome) throws Exception {
    exec("settings", "set", "-H", customHome.toString(), "read.key", "read.value");

    ProcessResult result = exec("settings", "get", "--streamx-home", customHome.toString(),
        "read.key");

    result.assertSuccess();
    assertThat(result.stdout().strip()).isEqualTo("read.value");
  }
}
