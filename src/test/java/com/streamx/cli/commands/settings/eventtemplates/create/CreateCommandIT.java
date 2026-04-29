package com.streamx.cli.commands.settings.eventtemplates.create;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.commands.publish.event.UserEventTemplates;
import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
class CreateCommandIT extends CliBaseIT {

  private static final ObjectMapper JSON = new ObjectMapper();

  @Test
  void shouldCreateTemplateFromWizard(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = execWithStdin(
        "my.new.template\ncom.example.my.new.template.v1\n",
        "settings", "event-templates", "create",
        "--streamx-home", home.toString()
    );

    result.assertSuccess();

    Path created = home.resolve(UserEventTemplates.DIRECTORY)
        .resolve("my.new.template.json");
    assertThat(created).isRegularFile();

    JsonNode root = JSON.readTree(Files.readString(created));
    assertThat(root.get("type").asText()).isEqualTo("com.example.my.new.template.v1");
    assertThat(root.get("id").asText()).isEqualTo("${uuid}");
    assertThat(root.get("specversion").asText()).isEqualTo("1.0");
    assertThat(root.get("source").asText()).isEqualTo("streamx-cli");
  }

  @Test
  void shouldWorkWithJsonOutput(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = execWithStdin(
        "my.json.template\ncom.example.my.json.template.v1\n",
        "settings", "event-templates", "create",
        "--streamx-home", home.toString(),
        "-o", "json"
    );

    result.assertSuccess();

    JsonNode root = JSON.readTree(result.stdout());
    assertThat(root.get("id").asText()).isEqualTo("my.json.template");
    assertThat(root.get("type").asText()).isEqualTo("com.example.my.json.template.v1");
    assertThat(root.get("path").asText()).endsWith("my.json.template.json");
  }

  @Test
  void shouldFailWhenTypeBlank(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = execWithStdin(
        "my.blank\n\n",
        "settings", "event-templates", "create",
        "--streamx-home", home.toString()
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("CloudEvent type is required");
    Path notCreated = home.resolve(UserEventTemplates.DIRECTORY).resolve("my.blank.json");
    assertThat(notCreated).doesNotExist();
  }

  @Test
  void shouldRepromptOnIdConflictAndContinueWithFreshId(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = home.resolve(UserEventTemplates.DIRECTORY);
    Files.createDirectories(userDir);
    Path existing = userDir.resolve("already.there.json");
    Files.writeString(existing, "{}");

    ProcessResult result = execWithStdin(

        "already.there\nfresh.id\ncom.example.fresh.v1\n",
        "settings", "event-templates", "create",
        "--streamx-home", home.toString()
    );

    result.assertSuccess();

    assertThat(result.stderr()).contains("already exists");
    assertThat(result.stderr()).contains(existing.toAbsolutePath().toString());
    assertThat(result.stderr()).contains("pick a different template ID");

    assertThat(Files.readString(existing)).isEqualTo("{}");

    Path created = userDir.resolve("fresh.id.json");
    assertThat(created).isRegularFile();
    JsonNode root = JSON.readTree(Files.readString(created));
    assertThat(root.get("type").asText()).isEqualTo("com.example.fresh.v1");
  }

  @Test
  void shouldRepromptOnConflictWithDefaultTemplate(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    exec("settings", "event-templates", "list",
        "--streamx-home", home.toString()).assertSuccess();

    ProcessResult result = execWithStdin(
        "page.published\nmy.custom\ncom.example.custom.v1\n",
        "settings", "event-templates", "create",
        "--streamx-home", home.toString()
    );

    result.assertSuccess();

    assertThat(result.stderr()).contains("already exists");
    assertThat(result.stderr())
        .contains(home.resolve("event-templates/default/page.published.json")
            .toAbsolutePath().toString());

    Path created = home.resolve(UserEventTemplates.DIRECTORY).resolve("my.custom.json");
    assertThat(created).isRegularFile();
  }

  @Test
  void shouldFailWhenInputExhaustedDuringConflictLoop(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = home.resolve(UserEventTemplates.DIRECTORY);
    Files.createDirectories(userDir);
    Files.writeString(userDir.resolve("already.there.json"), "{}");

    ProcessResult result = execWithStdin(
        "already.there\n",
        "settings", "event-templates", "create",
        "--streamx-home", home.toString()
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("already exists");
    assertThat(result.stderr()).contains("Template ID is required");
  }

  @Test
  void shouldFailWhenNameBlank(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = execWithStdin(
        "\n\n",
        "settings", "event-templates", "create",
        "--streamx-home", home.toString()
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("Template ID is required");
  }
}
