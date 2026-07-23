package com.streamx.cli.commands.settings.eventtemplates.delete;

import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.defaultTemplatesDir;
import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.profileFile;
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
class DeleteCommandIT extends CliBaseIT {

  private static final ObjectMapper JSON = new ObjectMapper();

  @Test
  void shouldWorkWithJsonOutput(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = userTemplatesDir(home);
    Files.createDirectories(userDir);
    Path target = userDir.resolve("my.thing.json");
    Files.writeString(target, sampleTemplate("com.example.thing.v1"));

    ProcessResult result = exec(
        "settings", "event-templates", "delete",
        "--streamx-home", home.toString(),
        "my.thing",
        "--yes",
        "-o", "json"
    );

    result.assertSuccess();
    JsonNode root = JSON.readTree(result.stdout());
    assertThat(root.get("id").asText()).isEqualTo("my.thing");
    assertThat(root.get("path").asText()).endsWith("my.thing.json");
    assertThat(target).doesNotExist();
  }

  @Test
  void shouldDeleteUserTemplateWithYesFlag(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = userTemplatesDir(home);
    Files.createDirectories(userDir);
    Path target = userDir.resolve("my.thing.json");
    Files.writeString(target, sampleTemplate("com.example.thing.v1"));

    ProcessResult result = exec(
        "settings", "event-templates", "delete",
        "--streamx-home", home.toString(),
        "my.thing",
        "--yes"
    );

    result.assertSuccess();
    assertThat(target).doesNotExist();
  }

  @Test
  void shouldDeleteUserTemplateAfterConfirmation(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = userTemplatesDir(home);
    Files.createDirectories(userDir);
    Path target = userDir.resolve("my.thing.json");
    Files.writeString(target, sampleTemplate("com.example.thing.v1"));

    ProcessResult result = execWithStdin(
        "y\n",
        "settings", "event-templates", "delete",
        "--streamx-home", home.toString(),
        "my.thing"
    );

    result.assertSuccess();
    assertThat(target).doesNotExist();
  }

  @Test
  void shouldCancelOnNo(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = userTemplatesDir(home);
    Files.createDirectories(userDir);
    Path target = userDir.resolve("my.thing.json");
    Files.writeString(target, sampleTemplate("com.example.thing.v1"));

    ProcessResult result = execWithStdin(
        "n\n",
        "settings", "event-templates", "delete",
        "--streamx-home", home.toString(),
        "my.thing"
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("Delete cancelled");
    assertThat(target).isRegularFile();
  }

  @Test
  void shouldRefuseDeleteOfDefaultTemplate(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    exec("settings", "event-templates", "list",
        "--streamx-home", home.toString()).assertSuccess();

    ProcessResult result = exec(
        "settings", "event-templates", "delete",
        "--streamx-home", home.toString(),
        "page.published",
        "--yes"
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("Cannot delete a default template");
    assertThat(result.stderr()).contains("reset-default-templates");
    Path defaultFile = defaultTemplatesDir(home).resolve("page.published.json");
    assertThat(defaultFile).isRegularFile();
  }

  @Test
  void shouldRefuseDeleteOfRegisteredTemplate(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Files.createDirectories(home);
    Path file = profileFile(home, "registered.json");
    Files.writeString(file, sampleTemplate("com.example.reg.v1"));
    exec("settings", "event-templates", "register",
        "--streamx-home", home.toString(),
        "my.alias", "registered.json").assertSuccess();

    ProcessResult result = exec(
        "settings", "event-templates", "delete",
        "--streamx-home", home.toString(),
        "my.alias",
        "--yes"
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("Cannot delete a registered template");
    assertThat(result.stderr()).contains("unregister");
    assertThat(file).isRegularFile();
  }

  @Test
  void shouldFailForUnknownTemplate(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "delete",
        "--streamx-home", home.toString(),
        "definitely.does.not.exist",
        "--yes"
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("definitely.does.not.exist");
  }
}
