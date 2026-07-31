package com.streamx.cli.commands.settings.eventtemplates.copy;

import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.defaultTemplatesDir;
import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.sampleTemplate;
import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.userTemplatesDir;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
class CopyCommandIT extends CliBaseIT {

  private static final ObjectMapper JSON = new ObjectMapper();

  @Test
  void shouldWorkWithJsonOutput(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    exec("settings", "event-templates", "list",
        "--streamx-home", home.toString()).assertSuccess();

    ProcessResult result = exec(
        "settings", "event-templates", "copy",
        "--streamx-home", home.toString(),
        "page.published",
        "my.page",
        "-o", "json"
    );

    result.assertSuccess();
    JsonNode root = JSON.readTree(result.stdout());
    assertThat(root.get("sourceId").asText()).isEqualTo("page.published");
    assertThat(root.get("destId").asText()).isEqualTo("my.page");
    assertThat(root.get("path").asText()).endsWith("my.page.json");
  }

  @Test
  void shouldCopyDefaultTemplateToUserFolder(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    exec("settings", "event-templates", "list",
        "--streamx-home", home.toString()).assertSuccess();

    ProcessResult result = exec(
        "settings", "event-templates", "copy",
        "--streamx-home", home.toString(),
        "page.published",
        "my.page"
    );

    result.assertSuccess();

    Path copy = userTemplatesDir(home).resolve("my.page.json");
    assertThat(copy).isRegularFile();
    String content = Files.readString(copy);
    assertThat(content).contains("com.streamx.blueprints.page.published");

    assertThat(defaultTemplatesDir(home).resolve("page.published.json")).isRegularFile();
  }

  @Test
  void shouldCopyUserTemplateUnderNewId(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = userTemplatesDir(home);
    Files.createDirectories(userDir);
    Path source = userDir.resolve("source.json");
    Files.writeString(source, sampleTemplate("com.example.source.v1"));

    ProcessResult result = exec(
        "settings", "event-templates", "copy",
        "--streamx-home", home.toString(),
        "source",
        "destination"
    );

    result.assertSuccess();
    Path copy = userDir.resolve("destination.json");
    assertThat(copy).isRegularFile();
    assertThat(Files.readString(copy)).isEqualTo(Files.readString(source));
  }

  @Test
  void shouldRefuseToOverwriteExistingId(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = userTemplatesDir(home);
    Files.createDirectories(userDir);
    Files.writeString(userDir.resolve("source.json"), sampleTemplate("com.example.source.v1"));
    Files.writeString(userDir.resolve("dest.json"), sampleTemplate("com.example.dest.v1"));

    ProcessResult result = exec(
        "settings", "event-templates", "copy",
        "--streamx-home", home.toString(),
        "source",
        "dest"
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("already exists");
  }

  @Test
  void shouldFailForUnknownSource(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "copy",
        "--streamx-home", home.toString(),
        "ghost",
        "new"
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("ghost");
  }

  @Test
  void shouldCopyViaInteractivePrompts(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    exec("settings", "event-templates", "list",
        "--streamx-home", home.toString()).assertSuccess();

    ProcessResult result = execWithStdin(
        "page.published\nmy.copy\n",
        "settings", "event-templates", "copy",
        "--streamx-home", home.toString()
    );

    result.assertSuccess();
    assertThat(userTemplatesDir(home).resolve("my.copy.json"))
        .isRegularFile();
  }
}
