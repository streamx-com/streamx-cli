package com.streamx.cli.commands.settings.eventtemplates.placeholders;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.commands.publish.EventTemplatePlaceholders;
import com.streamx.cli.test.CliBaseIT;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlaceholdersCommandIT extends CliBaseIT {

  private static final ObjectMapper JSON = new ObjectMapper();

  @Test
  void shouldWorkWithTextOutput(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "placeholders",
        "--streamx-home", home.toString()
    );

    result.assertSuccess();
    String stdout = result.stdout();
    assertThat(stdout).contains(EventTemplatePlaceholders.PAYLOAD_PATH);
    assertThat(stdout).contains(EventTemplatePlaceholders.SUBJECT);
    assertThat(stdout).contains(EventTemplatePlaceholders.UUID);
    assertThat(stdout).contains(EventTemplatePlaceholders.CURRENT_TIME);
  }

  @Test
  void shouldWorkWithJsonOutput(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "placeholders",
        "--streamx-home", home.toString(),
        "-o", "json"
    );

    result.assertSuccess();
    JsonNode root = JSON.readTree(result.stdout());
    assertThat(root.isArray()).isTrue();
    List<String> expected = List.of(
        EventTemplatePlaceholders.PAYLOAD_PATH,
        EventTemplatePlaceholders.PAYLOAD_CONTENT_BASE64,
        EventTemplatePlaceholders.PAYLOAD_CONTENT_JSON,
        EventTemplatePlaceholders.RELATIVE_PATH,
        EventTemplatePlaceholders.SUBJECT,
        EventTemplatePlaceholders.UUID,
        EventTemplatePlaceholders.CURRENT_TIME
    );
    for (int i = 0; i < expected.size(); i++) {
      JsonNode node = root.get(i);
      assertThat(node.get("name").asText()).isEqualTo(expected.get(i));
      assertThat(node.get("description").asText()).isNotBlank();
    }
  }

  @Test
  void shouldWorkWithYamlOutput(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "placeholders",
        "--streamx-home", home.toString(),
        "-o", "yaml"
    );

    result.assertSuccess();
    String stdout = result.stdout();
    assertThat(stdout).contains("name:");
    assertThat(stdout).contains("description:");
    assertThat(stdout).contains(EventTemplatePlaceholders.PAYLOAD_PATH);
  }
}
