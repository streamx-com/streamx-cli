package com.streamx.cli.commands.settings.eventtemplates.which;

import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.JSON;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.test.CliBaseIT;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WhichCommandIT extends CliBaseIT {

  @Test
  void shouldPrintAbsolutePathOfDefault(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    ProcessResult result = exec(
        "settings", "event-templates", "which",
        "--streamx-home", home.toString(),
        "page.published"
    );
    result.assertSuccess();
    String stdout = result.stdout().strip();
    assertThat(stdout).endsWith("default-event-templates/page.published.json");
    assertThat(Path.of(stdout)).isAbsolute();
  }

  @Test
  void shouldFailForUnknown(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    ProcessResult result = exec(
        "settings", "event-templates", "which",
        "--streamx-home", home.toString(),
        "ghost.template"
    );
    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("ghost.template");
  }

  @Test
  void shouldOutputJsonWithFullLocation(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    ProcessResult result = exec(
        "settings", "event-templates", "which",
        "--streamx-home", home.toString(),
        "page.published",
        "-o", "json"
    );
    result.assertSuccess();
    JsonNode root = JSON.readTree(result.stdout());
    assertThat(root.get("id").asText()).isEqualTo("page.published");
    assertThat(root.get("source").asText()).isEqualTo("default");
    assertThat(root.get("path").asText()).endsWith("page.published.json");
  }
}
