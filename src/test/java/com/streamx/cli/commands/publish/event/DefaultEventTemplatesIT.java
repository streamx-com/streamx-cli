package com.streamx.cli.commands.publish.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.ingestion.IngestionClientConfig;
import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.MeshTestSupport;
import com.streamx.cli.test.annotation.DisabledIfDockerUnavailable;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
@DisabledIfDockerUnavailable
public class DefaultEventTemplatesIT extends CliBaseIT {

  private static final String DEFAULT_TEMPLATE_TYPE = "page.published";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static String templateWithOrigin(String origin) {
    return """
        {
          "specversion": "1.0",
          "id": "${uuid}",
          "source": "test-source",
          "type": "com.streamx.blueprints.page.published.v1",
          "datacontenttype": "application/json",
          "subject": "${subject}",
          "time": "${currentTime}",
          "data": {
            "content": "file://${payloadPath}",
            "origin": "%s"
          }
        }
        """.formatted(origin);
  }

  @BeforeAll
  static void startMesh() {
    MeshTestSupport.startMesh("target/test-classes/mesh.yaml");
  }

  @AfterAll
  static void stopMesh() {
    MeshTestSupport.stopMesh();
  }

  @Test
  void shouldPopulateAllDefaultTemplatesOnFirstRun(@TempDir Path tempDir) throws Exception {
    Path customHome = tempDir.resolve("streamx-home");
    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html>hello</html>");

    seedIngestionUrl(customHome);

    exec(
        "publish", "event",
        "--streamx-home", customHome.toString(),
        DEFAULT_TEMPLATE_TYPE,
        payloadFile.toString()
    ).assertSuccess();

    Path dir = customHome.resolve(DefaultEventTemplates.DIRECTORY);
    assertThat(dir).isDirectory();
    List<String> expected = embeddedTemplateNames();
    assertThat(expected).isNotEmpty();
    for (String name : expected) {
      assertThat(dir.resolve(name + ".json"))
          .as("template file %s should be populated", name)
          .isRegularFile();
    }
  }

  @Test
  void shouldNotOverwriteUserModificationsOnSubsequentRuns(@TempDir Path tempDir) throws Exception {
    Path customHome = tempDir.resolve("streamx-home");
    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html>hello</html>");

    seedIngestionUrl(customHome);

    exec(
        "publish", "event",
        "--streamx-home", customHome.toString(),
        DEFAULT_TEMPLATE_TYPE,
        payloadFile.toString()
    ).assertSuccess();

    Path templateFile = customHome
        .resolve(DefaultEventTemplates.DIRECTORY)
        .resolve(DEFAULT_TEMPLATE_TYPE + ".json");
    assertThat(templateFile).isRegularFile();
    Files.writeString(templateFile, templateWithOrigin("user-modified"));

    ProcessResult result = exec(
        "publish", "event",
        "--streamx-home", customHome.toString(),
        DEFAULT_TEMPLATE_TYPE,
        payloadFile.toString(),
        "--output", "json"
    );

    result.assertSuccess();

    JsonNode data = MAPPER.readTree(result.stdout().strip()).get("event").get("data");
    assertThat(data.get("origin").asText()).isEqualTo("user-modified");
  }

  @Test
  void shouldUseUserCreatedTemplateFromStreamxHome(@TempDir Path tempDir) throws Exception {
    Path customHome = tempDir.resolve("streamx-home");
    Path templatesDir = customHome.resolve(DefaultEventTemplates.DIRECTORY);
    Files.createDirectories(templatesDir);
    Files.writeString(
        templatesDir.resolve(DEFAULT_TEMPLATE_TYPE + ".json"),
        templateWithOrigin("user-created")
    );

    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html>hello</html>");

    seedIngestionUrl(customHome);

    ProcessResult result = exec(
        "publish", "event",
        "--streamx-home", customHome.toString(),
        DEFAULT_TEMPLATE_TYPE,
        payloadFile.toString(),
        "--output", "json"
    );

    result.assertSuccess();

    JsonNode data = MAPPER.readTree(result.stdout().strip()).get("event").get("data");
    assertThat(data.get("origin").asText()).isEqualTo("user-created");
  }

  @Test
  void shouldRestoreDeletedDefaultTemplateOnNextRun(@TempDir Path tempDir) throws Exception {

    Path customHome = tempDir.resolve("streamx-home");
    Path templatesDir = customHome.resolve(DefaultEventTemplates.DIRECTORY);
    Files.createDirectories(templatesDir);

    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html>hello</html>");

    seedIngestionUrl(customHome);

    exec(
        "publish", "event",
        "--streamx-home", customHome.toString(),
        DEFAULT_TEMPLATE_TYPE,
        payloadFile.toString()
    ).assertSuccess();

    assertThat(templatesDir.resolve(DEFAULT_TEMPLATE_TYPE + ".json")).isRegularFile();
  }

  @Test
  void shouldPreferSettingsOverPopulatedTemplate(@TempDir Path tempDir) throws Exception {
    Path customHome = tempDir.resolve("streamx-home");
    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html>hello</html>");

    Files.createDirectories(customHome);
    Files.writeString(
        customHome.resolve("override-page-published.json"),
        templateWithOrigin("settings")
    );

    seedIngestionUrl(customHome);

    exec("settings", "set",
        "--streamx-home", customHome.toString(),
        "eventtemplate." + DEFAULT_TEMPLATE_TYPE,
        "override-page-published.json"
    ).assertSuccess();

    ProcessResult result = exec(
        "publish", "event",
        "--streamx-home", customHome.toString(),
        DEFAULT_TEMPLATE_TYPE,
        payloadFile.toString(),
        "--output", "json"
    );

    result.assertSuccess();

    JsonNode data = MAPPER.readTree(result.stdout().strip()).get("event").get("data");
    assertThat(data.get("origin").asText()).isEqualTo("settings");
  }

  private void seedIngestionUrl(Path customHome) throws Exception {
    if (!MeshTestSupport.isMeshActive()) {
      return;
    }
    exec("settings", "set",
        "--streamx-home", customHome.toString(),
        IngestionClientConfig.STREAMX_INGESTION_URL,
        "http://localhost:" + MeshTestSupport.getProxyPort()
    ).assertSuccess();
  }

  private static List<String> embeddedTemplateNames()
      throws IOException, URISyntaxException {
    Path resourceDir = Path.of(
        DefaultEventTemplatesIT.class
            .getResource("/" + DefaultEventTemplates.RESOURCE_DIRECTORY)
            .toURI()
    );
    try (Stream<Path> files = Files.list(resourceDir)) {
      return files
          .filter(Files::isRegularFile)
          .map(p -> p.getFileName().toString())
          .filter(name -> name.endsWith(DefaultEventTemplates.EXTENSION))
          .map(name -> name.substring(
              0, name.length() - DefaultEventTemplates.EXTENSION.length()))
          .toList();
    }
  }
}
