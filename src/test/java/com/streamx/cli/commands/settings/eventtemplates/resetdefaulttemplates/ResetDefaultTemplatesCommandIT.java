package com.streamx.cli.commands.settings.eventtemplates.resetdefaulttemplates;

import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.JSON;
import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.YAML;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.commands.publish.event.DefaultEventTemplates;
import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
class ResetDefaultTemplatesCommandIT extends CliBaseIT {

  private static Path defaultsDir(Path home) {
    return home.resolve(DefaultEventTemplates.DIRECTORY);
  }

  @Test
  void shouldCancelWhenUserDoesNotConfirm(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    exec("settings", "event-templates", "list",
        "--streamx-home", home.toString()).assertSuccess();

    Path defaultsDir = defaultsDir(home);
    Path sentinel = defaultsDir.resolve("page.published.json");
    Files.writeString(sentinel, "USER-EDITED");

    ProcessResult result = execWithStdin(
        "n\n",
        "settings", "event-templates", "reset-default-templates",
        "--streamx-home", home.toString()
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("Reset cancelled");

    assertThat(Files.readString(sentinel)).isEqualTo("USER-EDITED");
  }

  @Test
  void shouldCancelOnEmptyAnswer(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    exec("settings", "event-templates", "list",
        "--streamx-home", home.toString()).assertSuccess();

    Path sentinel = defaultsDir(home).resolve("page.published.json");
    Files.writeString(sentinel, "USER-EDITED");

    ProcessResult result = execWithStdin(
        "\n",
        "settings", "event-templates", "reset-default-templates",
        "--streamx-home", home.toString()
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("Reset cancelled");
    assertThat(Files.readString(sentinel)).isEqualTo("USER-EDITED");
  }

  @Test
  void shouldResetWhenConfirmedWithYes(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    exec("settings", "event-templates", "list",
        "--streamx-home", home.toString()).assertSuccess();

    Path defaultsDir = defaultsDir(home);
    Path sentinel = defaultsDir.resolve("page.published.json");
    Files.writeString(sentinel, "USER-EDITED");
    Path stray = defaultsDir.resolve("stray.json");
    Files.writeString(stray, "{}");

    ProcessResult result = execWithStdin(
        "y\n",
        "settings", "event-templates", "reset-default-templates",
        "--streamx-home", home.toString()
    );

    result.assertSuccess();

    assertThat(Files.readString(sentinel)).contains("com.streamx.blueprints.page.published");

    assertThat(stray).doesNotExist();

    for (String id : DefaultEventTemplates.templateNames()) {
      assertThat(defaultsDir.resolve(id + DefaultEventTemplates.EXTENSION)).isRegularFile();
    }
  }

  @Test
  void shouldResetWithoutPromptWhenYesFlagSet(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    exec("settings", "event-templates", "list",
        "--streamx-home", home.toString()).assertSuccess();

    Path sentinel = defaultsDir(home).resolve("asset.published.json");
    Files.writeString(sentinel, "USER-EDITED");

    ProcessResult result = exec(
        "settings", "event-templates", "reset-default-templates",
        "--streamx-home", home.toString(),
        "--yes"
    );

    result.assertSuccess();
    assertThat(Files.readString(sentinel)).contains("com.streamx.blueprints.asset.published");
  }

  @Test
  void shouldRecreateMissingDefaultsDir(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Files.createDirectories(home);

    assertThat(defaultsDir(home)).doesNotExist();

    ProcessResult result = exec(
        "settings", "event-templates", "reset-default-templates",
        "--streamx-home", home.toString(),
        "--yes"
    );

    result.assertSuccess();
    assertThat(defaultsDir(home)).isDirectory();
    for (String id : DefaultEventTemplates.templateNames()) {
      assertThat(defaultsDir(home).resolve(id + DefaultEventTemplates.EXTENSION)).isRegularFile();
    }
  }

  @Test
  void shouldOutputJson(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "reset-default-templates",
        "--streamx-home", home.toString(),
        "--yes",
        "--output", "json"
    );

    result.assertSuccess();
    JsonNode root = JSON.readTree(result.stdout());
    assertThat(root.get("path").asText())
        .isEqualTo(defaultsDir(home).toAbsolutePath().toString());
    JsonNode templates = root.get("templates");
    assertThat(templates.isArray()).isTrue();
    assertThat(templates.size()).isGreaterThanOrEqualTo(1);

    boolean foundPage = false;
    for (JsonNode t : templates) {
      if ("page.published".equals(t.asText())) {
        foundPage = true;
        break;
      }
    }
    assertThat(foundPage).isTrue();
  }

  @Test
  void shouldOutputYaml(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "reset-default-templates",
        "--streamx-home", home.toString(),
        "--yes",
        "--output", "yaml"
    );

    result.assertSuccess();
    JsonNode root = YAML.readTree(result.stdout());
    assertThat(root.has("path")).isTrue();
    assertThat(root.get("templates").isArray()).isTrue();
  }

  @Test
  void shouldNotTouchUserEventTemplatesFolder(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = home.resolve("event-templates/custom");
    Files.createDirectories(userDir);
    Path userFile = userDir.resolve("my.custom.json");
    Files.writeString(userFile, "{\"type\":\"user\"}");

    ProcessResult result = exec(
        "settings", "event-templates", "reset-default-templates",
        "--streamx-home", home.toString(),
        "--yes"
    );

    result.assertSuccess();
    assertThat(userFile).isRegularFile();
    assertThat(Files.readString(userFile)).isEqualTo("{\"type\":\"user\"}");
  }
}
