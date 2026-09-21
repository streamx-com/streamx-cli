package com.streamx.cli.commands.settings.eventtemplates.get;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.streamx.cli.test.CliBaseIT;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GetCommandIT extends CliBaseIT {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final YAMLMapper YAML = new YAMLMapper();

  @Test
  void shouldGetTemplateAsTextByName(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "get",
        "--streamx-home", home.toString(),
        "asset.published"
    );

    result.assertSuccess();
    JsonNode root = JSON.readTree(result.stdout());
    assertThat(root.get("type").asText())
        .isEqualTo("com.streamx.blueprints.asset.published.v1");
    assertThat(root.get("specversion").asText()).isEqualTo("1.0");
  }

  @Test
  void shouldGetTemplateAsJson(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "get",
        "--streamx-home", home.toString(),
        "page.published",
        "-o", "json"
    );

    result.assertSuccess();
    JsonNode root = JSON.readTree(result.stdout());
    assertThat(root.get("type").asText()).startsWith("com.streamx.blueprints.page.published");
  }

  @Test
  void shouldGetTemplateAsYaml(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "get",
        "--streamx-home", home.toString(),
        "page.published",
        "-o", "yaml"
    );

    result.assertSuccess();
    JsonNode root = YAML.readTree(result.stdout());
    assertThat(root.get("type").asText()).startsWith("com.streamx.blueprints.page.published");
  }

  @Test
  void shouldGetTemplateViaPrompt(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = execWithStdin(
        "page.published\n",
        "settings", "event-templates", "get",
        "--streamx-home", home.toString()
    );

    result.assertSuccess();
    JsonNode root = JSON.readTree(result.stdout());
    assertThat(root.get("type").asText()).startsWith("com.streamx.blueprints.page.published");
  }

  @Test
  void shouldFailForUnknownName(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "get",
        "--streamx-home", home.toString(),
        "definitely.not.a.template"
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("definitely.not.a.template");
  }
}
