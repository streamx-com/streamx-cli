package com.streamx.cli.commands.settings.eventtemplates.validate;

import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.JSON;
import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.sampleTemplate;
import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.userTemplatesDir;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
class ValidateCommandIT extends CliBaseIT {

  @Test
  void shouldValidateBundledDefault(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    ProcessResult result = exec(
        "settings", "event-templates", "validate",
        "--streamx-home", home.toString(),
        "page.published"
    );
    result.assertSuccess();
    assertThat(result.stdout()).contains("page.published").contains("valid");
  }

  @Test
  void shouldFailOnInvalidJson(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = userTemplatesDir(home);
    Files.createDirectories(userDir);
    Files.writeString(userDir.resolve("broken.json"), "{ this is not json");

    ProcessResult result = exec(
        "settings", "event-templates", "validate",
        "--streamx-home", home.toString(),
        "broken"
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stdout()).contains("invalid");
  }

  @Test
  void shouldFailOnMissingRequiredField(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = userTemplatesDir(home);
    Files.createDirectories(userDir);
    Files.writeString(userDir.resolve("nospec.json"), "{\"id\":\"x\",\"source\":\"s\"}");

    ProcessResult result = exec(
        "settings", "event-templates", "validate",
        "--streamx-home", home.toString(),
        "nospec"
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stdout()).contains("specversion");
  }

  @Test
  void shouldValidateAllWithFlag(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    exec("settings", "event-templates", "list",
        "--streamx-home", home.toString()).assertSuccess();

    ProcessResult result = exec(
        "settings", "event-templates", "validate",
        "--streamx-home", home.toString(),
        "--all",
        "-o", "json"
    );

    result.assertSuccess();
    JsonNode root = JSON.readTree(result.stdout());
    assertThat(root.get("validCount").asInt()).isGreaterThanOrEqualTo(8);
    assertThat(root.get("invalidCount").asInt()).isZero();
    assertThat(root.get("results").isArray()).isTrue();
  }

  @Test
  void shouldReportMixedResultsWithAllFlag(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    exec("settings", "event-templates", "list",
        "--streamx-home", home.toString()).assertSuccess();

    Path userDir = userTemplatesDir(home);
    Files.createDirectories(userDir);
    Files.writeString(userDir.resolve("good.json"), sampleTemplate("com.example.good.v1"));
    Files.writeString(userDir.resolve("bad.json"), "{}");

    ProcessResult result = exec(
        "settings", "event-templates", "validate",
        "--streamx-home", home.toString(),
        "--all",
        "-o", "json"
    );

    assertThat(result.exitCode()).isNotZero();
    JsonNode root = JSON.readTree(result.stdout());
    assertThat(root.get("validCount").asInt()).isGreaterThan(0);
    assertThat(root.get("invalidCount").asInt()).isGreaterThan(0);
  }
}
