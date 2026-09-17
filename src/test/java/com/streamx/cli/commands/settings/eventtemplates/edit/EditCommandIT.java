package com.streamx.cli.commands.settings.eventtemplates.edit;

import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.defaultTemplatesDir;
import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.userTemplatesDir;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
class EditCommandIT extends CliBaseIT {

  private static final ObjectMapper JSON = new ObjectMapper();

  @BeforeEach
  void setNoOpEditor() {

    String editor = Files.exists(Path.of("/usr/bin/true")) ? "/usr/bin/true" : "true";
    setEnv(EditCommand.EDITOR, editor);
  }

  @AfterEach
  void clearEditor() {
    clearEnv(EditCommand.EDITOR);
  }

  @Test
  void shouldWorkWithJsonOutput(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = userTemplatesDir(home);
    Files.createDirectories(userDir);
    Path target = userDir.resolve("my.thing.json");
    Files.writeString(target, "{\"type\":\"x\"}");

    ProcessResult result = exec(
        "settings", "event-templates", "edit",
        "--streamx-home", home.toString(),
        "my.thing",
        "-o", "json"
    );

    result.assertSuccess();
    JsonNode root = JSON.readTree(result.stdout());
    assertThat(root.get("id").asText()).isEqualTo("my.thing");
    assertThat(root.get("path").asText()).endsWith("my.thing.json");
    assertThat(root.get("editor").asText()).isNotBlank();
  }

  @Test
  void shouldCopyDefaultIntoUserFolderOnFirstEdit(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "edit",
        "--streamx-home", home.toString(),
        "asset.published"
    );

    result.assertSuccess();

    Path userCopy = userTemplatesDir(home).resolve("asset.published.json");
    assertThat(userCopy).isRegularFile();
    String content = Files.readString(userCopy);
    assertThat(content).contains("com.streamx.blueprints.asset.published.v1");

    Path defaultFile = defaultTemplatesDir(home).resolve("asset.published.json");
    assertThat(defaultFile).isRegularFile();
  }

  @Test
  void shouldEditUserTemplateInPlace(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = userTemplatesDir(home);
    Files.createDirectories(userDir);
    Path target = userDir.resolve("my.thing.json");
    Files.writeString(target, "{\"type\":\"x\"}");

    ProcessResult result = exec(
        "settings", "event-templates", "edit",
        "--streamx-home", home.toString(),
        "my.thing"
    );

    result.assertSuccess();
    assertThat(target).isRegularFile();
    assertThat(Files.readString(target)).isEqualTo("{\"type\":\"x\"}");
  }

  @Test
  void shouldEditViaPrompt(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = execWithStdin(
        "page.published\n",
        "settings", "event-templates", "edit",
        "--streamx-home", home.toString()
    );

    result.assertSuccess();
    Path userCopy = userTemplatesDir(home).resolve("page.published.json");
    assertThat(userCopy).isRegularFile();
  }

  @Test
  void shouldFailForUnknownName(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "edit",
        "--streamx-home", home.toString(),
        "definitely.not.a.template"
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("definitely.not.a.template");
  }
}
