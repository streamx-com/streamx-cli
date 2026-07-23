package com.streamx.cli.commands.settings.eventtemplates.list;

import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.JSON;
import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.YAML;
import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.findById;
import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.sampleTemplate;
import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.userTemplatesDir;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
class ListCommandIT extends CliBaseIT {

  private static final String SOURCE_DEFAULT = EventTemplateCatalog.SOURCE_DEFAULT;
  private static final String SOURCE_CUSTOM = EventTemplateCatalog.SOURCE_CUSTOM;
  private static final String SOURCE_SETTINGS = EventTemplateCatalog.SOURCE_SETTINGS;

  @Test
  void shouldListBuiltinTemplatesAsJson(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "list",
        "--streamx-home", home.toString(),
        "--output", "json"
    );

    result.assertSuccess();

    JsonNode root = JSON.readTree(result.stdout());
    assertThat(root.get("streamxHome").asText()).isEqualTo(home.toAbsolutePath().toString());

    JsonNode templates = root.get("templates");
    assertThat(templates.isArray()).isTrue();
    assertThat(templates.size()).isGreaterThanOrEqualTo(1);

    JsonNode pagePublished = findById(templates, "page.published");
    assertThat(pagePublished).as("page.published should be in the listing").isNotNull();
    assertThat(pagePublished.get("source").asText()).isEqualTo(SOURCE_DEFAULT);
    assertThat(pagePublished.get("type").asText())
        .startsWith("com.streamx.blueprints.page.published");
  }

  @Test
  void shouldShowUserTemplateFromEventTemplatesFolder(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = userTemplatesDir(home);
    Files.createDirectories(userDir);
    Files.writeString(userDir.resolve("my.custom.json"), sampleTemplate("com.example.custom.v1"));

    ProcessResult result = exec(
        "settings", "event-templates", "list",
        "--streamx-home", home.toString(),
        "--output", "json"
    );

    result.assertSuccess();
    JsonNode entry = findById(JSON.readTree(result.stdout()).get("templates"), "my.custom");
    assertThat(entry).isNotNull();
    assertThat(entry.get("source").asText()).isEqualTo(SOURCE_CUSTOM);
    assertThat(entry.get("type").asText()).isEqualTo("com.example.custom.v1");
  }

  @Test
  void shouldPrioritizeUserOverDefaults(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    exec("settings", "event-templates", "list",
        "--streamx-home", home.toString()).assertSuccess();

    Path userDir = userTemplatesDir(home);
    Files.createDirectories(userDir);
    Files.writeString(
        userDir.resolve("page.published.json"),
        sampleTemplate("com.example.override.page.v9"));

    ProcessResult result = exec(
        "settings", "event-templates", "list",
        "--streamx-home", home.toString(),
        "--output", "json"
    );
    result.assertSuccess();

    JsonNode templates = JSON.readTree(result.stdout()).get("templates");
    int matches = 0;
    JsonNode pagePublished = null;
    for (JsonNode t : templates) {
      if ("page.published".equals(t.get("id").asText())) {
        matches++;
        pagePublished = t;
      }
    }
    assertThat(matches).isEqualTo(1);
    assertThat(pagePublished.get("source").asText()).isEqualTo(SOURCE_CUSTOM);
    assertThat(pagePublished.get("type").asText()).isEqualTo("com.example.override.page.v9");
  }

  @Test
  void shouldPrioritizeSettingsOverUserAndDefaults(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    exec("settings", "event-templates", "list",
        "--streamx-home", home.toString()).assertSuccess();

    Path userDir = userTemplatesDir(home);
    Files.createDirectories(userDir);
    Files.writeString(
        userDir.resolve("page.published.json"),
        sampleTemplate("com.example.user.page.v1"));

    Path settingsFile = home.resolve("profiles/default/page-from-settings.json");
    Files.writeString(settingsFile, sampleTemplate("com.example.settings.page.v1"));

    exec("settings", "event-templates", "register",
        "--streamx-home", home.toString(),
        "page.published", "page-from-settings.json"
    ).assertSuccess();

    ProcessResult result = exec(
        "settings", "event-templates", "list",
        "--streamx-home", home.toString(),
        "--output", "json"
    );
    result.assertSuccess();

    JsonNode pagePublished = findById(JSON.readTree(result.stdout()).get("templates"),
        "page.published");
    assertThat(pagePublished.get("source").asText()).isEqualTo(SOURCE_SETTINGS);
    assertThat(pagePublished.get("type").asText()).isEqualTo("com.example.settings.page.v1");
  }

  @Test
  void shouldRenderTextOutput(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    ProcessResult result = exec("settings", "event-templates", "list",
        "--streamx-home", home.toString());
    result.assertSuccess();
    assertThat(result.stdout()).contains("TEMPLATE ID");
    assertThat(result.stdout()).contains("page.published");
  }

  @Test
  void shouldRenderYamlOutput(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    ProcessResult result = exec("settings", "event-templates", "list",
        "--streamx-home", home.toString(),
        "--output", "yaml");
    result.assertSuccess();
    JsonNode root = YAML.readTree(result.stdout());
    assertThat(root.has("streamxHome")).isTrue();
    assertThat(root.get("templates").isArray()).isTrue();
  }
}
