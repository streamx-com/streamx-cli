package com.streamx.cli.commands.settings.eventtemplates.unregister;

import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.configFile;
import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.defaultTemplatesDir;
import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.profileFile;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.commands.publish.event.EventTemplateLoader;
import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
class UnregisterCommandIT extends CliBaseIT {

  @Test
  void shouldRemoveSettingsEntryByName(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Files.createDirectories(home);
    Files.writeString(profileFile(home, "custom.json"), "{}");

    exec("settings", "event-templates", "register",
        "--streamx-home", home.toString(),
        "my.alias", "custom.json"
    ).assertSuccess();

    ProcessResult result = exec(
        "settings", "event-templates", "unregister",
        "--streamx-home", home.toString(),
        "my.alias"
    );

    result.assertSuccess();

    Properties props = readConfig(home);
    assertThat(props.getProperty(
        EventTemplateLoader.TEMPLATE_SETTINGS_MAPPING_PREFIX + "my.alias")).isNull();
  }

  @Test
  void shouldRemoveSettingsEntryViaPrompt(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Files.createDirectories(home);
    Files.writeString(profileFile(home, "a.json"), "{}");
    Files.writeString(profileFile(home, "b.json"), "{}");

    exec("settings", "event-templates", "register",
        "--streamx-home", home.toString(),
        "alias.a", "a.json").assertSuccess();
    exec("settings", "event-templates", "register",
        "--streamx-home", home.toString(),
        "alias.b", "b.json").assertSuccess();

    ProcessResult result = execWithStdin(
        "alias.a\n",
        "settings", "event-templates", "unregister",
        "--streamx-home", home.toString()
    );

    result.assertSuccess();

    Properties props = readConfig(home);
    assertThat(props.getProperty(
        EventTemplateLoader.TEMPLATE_SETTINGS_MAPPING_PREFIX + "alias.a")).isNull();
    assertThat(props.getProperty(
        EventTemplateLoader.TEMPLATE_SETTINGS_MAPPING_PREFIX + "alias.b")).isEqualTo("b.json");
  }

  @Test
  void shouldRefuseUnknownName(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Files.createDirectories(home);
    Files.writeString(profileFile(home, "a.json"), "{}");
    exec("settings", "event-templates", "register",
        "--streamx-home", home.toString(),
        "real.one", "a.json").assertSuccess();

    ProcessResult result = exec(
        "settings", "event-templates", "unregister",
        "--streamx-home", home.toString(),
        "ghost.alias"
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("ghost.alias");
  }

  @Test
  void shouldRefuseWhenNoRegistrationsExist(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "unregister",
        "--streamx-home", home.toString()
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("No event templates are registered");
  }

  @Test
  void shouldNotTouchDefaultsFolder(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Files.createDirectories(home);
    Files.writeString(profileFile(home, "a.json"), "{}");

    exec("settings", "event-templates", "register",
        "--streamx-home", home.toString(),
        "my.alias", "a.json").assertSuccess();

    Path defaultPagePublished = defaultTemplatesDir(home).resolve("page.published.json");
    assertThat(defaultPagePublished).isRegularFile();

    exec("settings", "event-templates", "unregister",
        "--streamx-home", home.toString(),
        "my.alias").assertSuccess();

    assertThat(defaultPagePublished).isRegularFile();
  }

  private static Properties readConfig(Path home) throws Exception {
    Path config = configFile(home);
    Properties props = new Properties();
    try (InputStream is = Files.newInputStream(config)) {
      props.load(is);
    }
    return props;
  }
}
