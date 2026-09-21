package com.streamx.cli.commands.settings.list;

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

class ListCommandIT extends CliBaseIT {

  Map<String, String> testProperties = Map.of(
      "test.key", "test.value",
      "another.key", "another.value",
      "special.chars", "value=with:special@chars!",
      "empty.value", "",
      "spaced.value", "value with spaces"
  );

  @BeforeEach
  void clearConfig() throws IOException {
    Files.deleteIfExists(getConfigPath());
  }

  private void writeProperties(Map<String, String> propertiesToWrite) throws IOException {
    Properties initialProps = new Properties();
    for (Map.Entry<String, String> entry : propertiesToWrite.entrySet()) {
      initialProps.setProperty(entry.getKey(), entry.getValue());
    }
    Path configFile = getConfigPath();
    Files.createDirectories(configFile.getParent());
    try (OutputStream out = Files.newOutputStream(configFile)) {
      initialProps.store(out, null);
    }
  }

  @Test
  void shouldFormatOutputAsText() throws Exception {
    writeProperties(testProperties);

    ProcessResult result = exec("settings", "list");

    String expectedOutput = """
        KEY            VALUE
        another.key    another.value
        empty.value    -
        spaced.value   value with spaces
        special.chars  value=with:special@chars!
        test.key       test.value
        """.strip();

    assertThat(result.stdout().strip()).isEqualTo(expectedOutput);
    assertThat(result.stderr()).isEmpty();
    result.assertSuccess();
  }

  @Test
  void shouldFormatEmptyOutputAsText() throws Exception {
    writeProperties(Map.of());

    ProcessResult result = exec("settings", "list");

    assertThat(result.stdout().strip()).isEqualTo(msg.listSettingsNoPropertiesFound());
    assertThat(result.stderr()).isEmpty();
    result.assertSuccess();
  }

  @Test
  void shouldFormatOutputAsJson() throws Exception {
    writeProperties(testProperties);

    ProcessResult result = exec("settings", "list", "--output", "json");

    String expectedOutput = """
        {
          "special.chars" : "value=with:special@chars!",
          "empty.value" : "",
          "spaced.value" : "value with spaces",
          "test.key" : "test.value",
          "another.key" : "another.value"
        }
        """.strip();

    assertThat(result.stdout().strip()).isEqualTo(expectedOutput);
    assertThat(result.stderr()).isEmpty();
    result.assertSuccess();
  }

  @Test
  void shouldFormatEmptyOutputAsJson() throws Exception {
    writeProperties(Map.of());

    ProcessResult result = exec("settings", "list", "--output", "json");

    assertThat(result.stdout().strip()).isEqualTo("{ }");
    assertThat(result.stderr()).isEmpty();
    result.assertSuccess();
  }

  @Test
  void shouldFormatOutputAsYaml() throws Exception {
    writeProperties(testProperties);

    ProcessResult result = exec("settings", "list", "--output", "yaml");

    String expectedOutput = """
        special.chars: "value=with:special@chars!"
        empty.value: ""
        spaced.value: "value with spaces"
        test.key: "test.value"
        another.key: "another.value"
        """.strip();

    assertThat(result.stdout().strip()).isEqualTo(expectedOutput);
    assertThat(result.stderr()).isEmpty();
    result.assertSuccess();
  }

  @Test
  void shouldFormatEmptyOutputAsYaml() throws Exception {
    writeProperties(Map.of());

    ProcessResult result = exec("settings", "list", "--output", "yaml");

    assertThat(result.stdout().strip()).isEqualTo("{}");
    assertThat(result.stderr()).isEmpty();
    result.assertSuccess();
  }
}
