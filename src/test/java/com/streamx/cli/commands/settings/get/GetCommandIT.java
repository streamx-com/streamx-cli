package com.streamx.cli.commands.settings.get;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.test.CliBaseIT;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetCommandIT extends CliBaseIT {

  Map<String, String> testProperties = Map.of(
      "test.key", "test.value",
      "another.key", "another.value",
      "special.chars", "value=with:special@chars!",
      "empty.value", "",
      "spaced.value", "value with spaces"
  );

  @BeforeEach
  void writeConfig() throws IOException {
    Files.deleteIfExists(getConfigPath());
    Properties initialProps = new Properties();
    for (Map.Entry<String, String> property : testProperties.entrySet()) {
      initialProps.setProperty(property.getKey(), property.getValue());
    }
    Path configFile = getConfigPath();
    Files.createDirectories(configFile.getParent());
    try (OutputStream out = Files.newOutputStream(configFile)) {
      initialProps.store(out, null);
    }
  }

  @Test
  void shouldDisplayPropertyIfExists() throws Exception {
    for (Map.Entry<String, String> property : testProperties.entrySet()) {
      String key = property.getKey();
      String value = property.getValue();

      // With text output
      ProcessResult result = exec("settings", "get", key);
      assertThat(result.stdout().strip()).isEqualTo(value);
      assertThat(result.stderr()).isEmpty();
      result.assertSuccess();

      // With JSON output
      ProcessResult jsonResult = exec("settings", "get", "--output", "json", key);
      String expectedJsonValue = """
          "%s"
          """.strip().formatted(value);
      assertThat(jsonResult.stdout().strip()).isEqualTo(expectedJsonValue);
      assertThat(jsonResult.stderr()).isEmpty();
      jsonResult.assertSuccess();

      // With YAML output
      ProcessResult yamlResult = exec("settings", "get", "--output", "yaml", key);
      String expectedYamlValue = """
          "%s"
          """.strip().formatted(value);
      assertThat(yamlResult.stdout().strip()).isEqualTo(expectedYamlValue);
      assertThat(yamlResult.stderr()).isEmpty();
      yamlResult.assertSuccess();
    }
  }

  @Test
  void shouldFailIfNoPropertyFound() throws Exception {
    String key = "non.existing.key";
    ProcessResult result = exec("settings", "get", key);

    assertThat(result.stdout()).isEmpty();
    assertThat(result.stderr()).contains(msg.noSettingsPropertyFound(key));
    result.assertExitCode(1);
  }
}
