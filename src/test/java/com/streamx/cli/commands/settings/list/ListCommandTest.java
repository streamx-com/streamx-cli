package com.streamx.cli.commands.settings.list;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.streamx.cli.config.DotStreamxConfigSource;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusMainTest
class ListCommandTest {
  private Path configFile;

  Map<String, String> testProperties = Map.of(
      "test.key", "test.value",
      "another.key", "another.value",
      "special.chars", "value=with:special@chars!",
      "empty.value", "",
      "spaced.value", "value with spaces"
  );

  @BeforeEach
  void setUp() throws IOException, URISyntaxException {
    Path tempDir = Files.createTempDirectory("SetCommandTest");
    System.setProperty("user.home", tempDir.toString());
    configFile = new File(DotStreamxConfigSource.getUrl().toURI()).toPath();
  }

  private void writeProperties(Map<String, String> propertiesToWrite) throws IOException {
    Properties initialProps = new Properties();
    for (Map.Entry<String, String> entry : propertiesToWrite.entrySet()) {
      initialProps.setProperty(entry.getKey(), entry.getValue());
    }

    try (OutputStream out = Files.newOutputStream(configFile)) {
      initialProps.store(out, null);
    }
  }

  @Test
  void shouldFormatOutputAsText(QuarkusMainLauncher launcher) throws IOException {
    writeProperties(testProperties);

    LaunchResult launchResult = launcher.launch("settings", "list");

    String expectedOutput = msg.listSettingsHeader() + "\n" + """
        another.key   = another.value
        empty.value   =
        spaced.value  = value with spaces
        special.chars = value=with:special@chars!
        test.key      = test.value
        """.strip();

    assertEquals(expectedOutput, launchResult.getOutput());
    assertEquals("", launchResult.getErrorOutput());
    assertEquals(0, launchResult.exitCode());
  }

  @Test
  void shouldFormatEmptyOutputAsText(QuarkusMainLauncher launcher) throws Exception {
    writeProperties(Map.of());

    LaunchResult launchResult = launcher.launch("settings", "list");

    String expectedOutput = msg.listSettingsNoPropertiesFound();

    assertEquals(expectedOutput, launchResult.getOutput());
    assertEquals("", launchResult.getErrorOutput());
    assertEquals(0, launchResult.exitCode());
  }

  @Test
  void shouldFormatOutputAsJson(QuarkusMainLauncher launcher) throws Exception {
    writeProperties(testProperties);

    LaunchResult launchResult = launcher.launch("settings", "list", "--output", "json");

    String expectedOutput = """
        {
          "special.chars" : "value=with:special@chars!",
          "empty.value" : "",
          "spaced.value" : "value with spaces",
          "test.key" : "test.value",
          "another.key" : "another.value"
        }
        """.strip();

    assertEquals(expectedOutput, launchResult.getOutput());
    assertEquals("", launchResult.getErrorOutput());
    assertEquals(0, launchResult.exitCode());
  }

  @Test
  void shouldFormatEmptyOutputAsJson(QuarkusMainLauncher launcher) throws Exception {
    writeProperties(Map.of());

    LaunchResult launchResult = launcher.launch("settings", "list", "--output", "json");

    String expectedOutput = "{ }".strip();

    assertEquals(expectedOutput, launchResult.getOutput());
    assertEquals("", launchResult.getErrorOutput());
    assertEquals(0, launchResult.exitCode());
  }

  @Test
  void shouldFormatOutputAsYaml(QuarkusMainLauncher launcher) throws Exception {
    writeProperties(testProperties);

    LaunchResult launchResult = launcher.launch("settings", "list", "--output", "yaml");

    String expectedOutput = """
        special.chars: "value=with:special@chars!"
        empty.value: ""
        spaced.value: "value with spaces"
        test.key: "test.value"
        another.key: "another.value"
        """.strip();

    assertEquals(expectedOutput, launchResult.getOutput());
    assertEquals("", launchResult.getErrorOutput());
    assertEquals(0, launchResult.exitCode());
  }

  @Test
  void shouldFormatEmptyOutputAsYaml(QuarkusMainLauncher launcher) throws Exception {
    writeProperties(Map.of());

    LaunchResult launchResult = launcher.launch("settings", "list", "--output", "yaml");

    String expectedOutput = "{}".strip();

    assertEquals(expectedOutput, launchResult.getOutput());
    assertEquals("", launchResult.getErrorOutput());
    assertEquals(0, launchResult.exitCode());
  }
}