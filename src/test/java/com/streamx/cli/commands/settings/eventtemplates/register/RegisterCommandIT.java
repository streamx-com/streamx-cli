package com.streamx.cli.commands.settings.eventtemplates.register;

import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.configFile;
import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.contextFile;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.commands.publish.event.EventTemplateLoader;
import com.streamx.cli.test.CliBaseIT;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RegisterCommandIT extends CliBaseIT {

  @Test
  void shouldWriteSettingsEntry(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Files.createDirectories(home);
    Path templateFile = contextFile(home, "custom.json");
    Files.writeString(templateFile, "{}");

    ProcessResult result = exec(
        "settings", "event-templates", "register",
        "--streamx-home", home.toString(),
        "my.alias",
        "custom.json"
    );

    result.assertSuccess();

    Path config = configFile(home);
    assertThat(config).isRegularFile();
    Properties props = new Properties();
    try (InputStream is = Files.newInputStream(config)) {
      props.load(is);
    }
    assertThat(props.getProperty(
        EventTemplateLoader.TEMPLATE_SETTINGS_MAPPING_PREFIX + "my.alias"))
        .isEqualTo("custom.json");
  }

  @Test
  void shouldFailWhenArgsMissing(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "register",
        "--streamx-home", home.toString()
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("Missing required parameter");
  }
}
