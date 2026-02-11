package com.streamx.cli.commands.settings.get;

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
class GetCommandTest {
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
    Path configFile = new File(DotStreamxConfigSource.getUrl().toURI()).toPath();

    Properties initialProps = new Properties();
    for (Map.Entry<String, String> property : testProperties.entrySet()) {
      initialProps.setProperty(property.getKey(), property.getValue());
    }

    try (OutputStream out = Files.newOutputStream(configFile)) {
      initialProps.store(out, null);
    }
  }

  @Test
  void shouldDisplayPropertyIfExists(QuarkusMainLauncher launcher) {
    for (Map.Entry<String, String> property : testProperties.entrySet()) {
      String key = property.getKey();
      String value = property.getValue();

      // With text output
      LaunchResult launchResult = launcher.launch("settings", "get", key);

      assertEquals(value, launchResult.getOutput());
      assertEquals("", launchResult.getErrorOutput());
      assertEquals(0, launchResult.exitCode());

      // With JSON output
      LaunchResult jsonLaunchResult = launcher.launch("settings", "get", "--output", "json", key);
      String expectedJsonValue = """
          "%s"
          """.strip().formatted(value);

      assertEquals(expectedJsonValue, jsonLaunchResult.getOutput());
      assertEquals("", jsonLaunchResult.getErrorOutput());
      assertEquals(0, jsonLaunchResult.exitCode());

      // With YAML output
      LaunchResult yamlLaunchResult = launcher.launch("settings", "get", "--output", "yaml", key);
      String expectedYamlValue = """
          "%s"
          """.strip().formatted(value);

      assertEquals(expectedYamlValue, yamlLaunchResult.getOutput());
      assertEquals("", yamlLaunchResult.getErrorOutput());
      assertEquals(0, yamlLaunchResult.exitCode());
    }
  }

  @Test
  void shouldFailIfNoPropertyFound(QuarkusMainLauncher launcher) {
    String key = "non.existing.key";
    LaunchResult launchResult = launcher.launch("settings", "get", key);

    assertEquals("", launchResult.getOutput());
    assertEquals(msg.noSettingsPropertyFound(key), launchResult.getErrorOutput());
    assertEquals(1, launchResult.exitCode());
  }
}