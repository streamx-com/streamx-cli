package com.streamx.cli.commands.settings.eventtemplates.rename;

import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.configFile;
import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.contextFile;
import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.sampleTemplate;
import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.userTemplatesDir;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class RenameCommandIT extends CliBaseIT {

  private static final ObjectMapper JSON = new ObjectMapper();

  @Test
  void shouldWorkWithJsonOutput(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = userTemplatesDir(home);
    Files.createDirectories(userDir);
    Files.writeString(userDir.resolve("old.json"), sampleTemplate("com.example.v1"));

    ProcessResult result = exec(
        "settings", "event-templates", "rename",
        "--streamx-home", home.toString(),
        "old", "renamed",
        "-o", "json"
    );

    result.assertSuccess();
    JsonNode root = JSON.readTree(result.stdout());
    assertThat(root.get("oldId").asText()).isEqualTo("old");
    assertThat(root.get("newId").asText()).isEqualTo("renamed");
    assertThat(root.get("source").asText()).isNotBlank();
    assertThat(root.get("path").asText()).endsWith("renamed.json");
  }

  @Test
  void shouldRenameUserTemplateFile(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = userTemplatesDir(home);
    Files.createDirectories(userDir);
    Path original = userDir.resolve("old.json");
    String content = sampleTemplate("com.example.v1");
    Files.writeString(original, content);

    ProcessResult result = exec(
        "settings", "event-templates", "rename",
        "--streamx-home", home.toString(),
        "old", "renamed"
    );

    result.assertSuccess();
    assertThat(original).doesNotExist();
    Path renamed = userDir.resolve("renamed.json");
    assertThat(renamed).isRegularFile();
    assertThat(Files.readString(renamed)).isEqualTo(content);
  }

  @Test
  void shouldRenameSettingsRegisteredTemplate(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Files.createDirectories(home);
    Path file = contextFile(home, "registered.json");
    Files.writeString(file, sampleTemplate("com.example.reg.v1"));
    exec("settings", "event-templates", "register",
        "--streamx-home", home.toString(),
        "old.alias", "registered.json").assertSuccess();

    ProcessResult result = exec(
        "settings", "event-templates", "rename",
        "--streamx-home", home.toString(),
        "old.alias", "new.alias"
    );

    result.assertSuccess();

    Properties props = readConfig(home);
    assertThat(props.getProperty(
        EventTemplateLoader.TEMPLATE_SETTINGS_MAPPING_PREFIX + "old.alias")).isNull();
    assertThat(props.getProperty(
        EventTemplateLoader.TEMPLATE_SETTINGS_MAPPING_PREFIX + "new.alias"))
        .isEqualTo("registered.json");

    assertThat(file).isRegularFile();
  }

  @Test
  void shouldRefuseToRenameDefault(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    exec("settings", "event-templates", "list",
        "--streamx-home", home.toString()).assertSuccess();

    ProcessResult result = exec(
        "settings", "event-templates", "rename",
        "--streamx-home", home.toString(),
        "page.published", "my.page"
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("Cannot rename a default template");
  }

  @Test
  void shouldRefuseToRenameWhenNewIdAlreadyExists(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = userTemplatesDir(home);
    Files.createDirectories(userDir);
    Files.writeString(userDir.resolve("a.json"), sampleTemplate("com.example.a.v1"));
    Files.writeString(userDir.resolve("b.json"), sampleTemplate("com.example.b.v1"));

    ProcessResult result = exec(
        "settings", "event-templates", "rename",
        "--streamx-home", home.toString(),
        "a", "b"
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("already exists");
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
