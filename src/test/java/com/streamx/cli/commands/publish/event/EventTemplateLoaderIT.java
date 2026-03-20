package com.streamx.cli.commands.publish.event;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.annotation.DisabledIfDockerUnavailable;
import com.streamx.cli.test.profiles.DefaultMeshTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
@DisabledIfDockerUnavailable
@TestProfile(DefaultMeshTestProfile.class)
public class EventTemplateLoaderIT extends CliBaseIT {

  private static final String TEMPLATE_TYPE = "path.resolution.template";
  private static final String DEFAULT_TEMPLATE_TYPE = "page.published";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static String createTemplateContent() {
    return """
        {
          "specversion": "1.0",
          "id": "test-id",
          "source": "test-source",
          "type": "com.streamx.blueprints.page.published.v1",
          "datacontenttype": "application/json",
          "subject": "${subject}",
          "time": "2026-01-01T00:00:00.000000Z",
          "data": {"content": "${payloadPath}", "type": "data/page", "origin": "settings"}
        }
        """;
  }

  private void registerTemplate(String relativePath) throws Exception {
    registerTemplate(TEMPLATE_TYPE, relativePath);
  }

  private void registerTemplate(String eventType, String relativePath) throws Exception {
    exec("settings", "set",
        "eventtemplate." + eventType,
        relativePath
    ).assertSuccess();
  }

  private void registerTemplateAbsolute(Path absolutePath) throws Exception {
    registerTemplateAbsolute(TEMPLATE_TYPE, absolutePath);
  }

  private void registerTemplateAbsolute(String eventType, Path absolutePath) throws Exception {
    exec("settings", "set",
        "eventtemplate." + eventType,
        absolutePath.toString()
    ).assertSuccess();
  }

  private void unregisterTemplate(String eventType) throws Exception {
    exec("settings", "unset",
        "eventtemplate." + eventType
    ).assertSuccess();
  }

  @Test
  void shouldResolveTemplateFromRelativeFilename(@TempDir Path tempDir) throws Exception {
    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html>hello</html>");

    Path templateFile = streamxHome.resolve("relative-test-template.json");
    Files.writeString(templateFile, createTemplateContent());

    try {
      registerTemplate("relative-test-template.json");

      ProcessResult result = exec(
          "publish", "event",
          TEMPLATE_TYPE,
          payloadFile.toString(),
          "--output", "json"
      );

      result.assertSuccess();

      JsonNode event = MAPPER.readTree(result.stdout().strip());
      JsonNode data = event.get("event").get("data");
      assertThat(data.get("content").asText()).isEqualTo(payloadFile.toString());
      assertThat(data.get("type").asText()).isEqualTo("data/page");
    } finally {
      Files.deleteIfExists(templateFile);
    }
  }

  @Test
  void shouldResolveTemplateFromRelativeSubdirectoryPath(@TempDir Path tempDir) throws Exception {
    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html>hello</html>");

    Path templateSubDir = streamxHome.resolve("templates");
    Files.createDirectories(templateSubDir);
    Path templateFile = templateSubDir.resolve("sub-template.json");
    Files.writeString(templateFile, createTemplateContent());

    try {
      registerTemplate("templates/sub-template.json");

      ProcessResult result = exec(
          "publish", "event",
          TEMPLATE_TYPE,
          payloadFile.toString(),
          "--output", "json"
      );

      result.assertSuccess();

      JsonNode event = MAPPER.readTree(result.stdout().strip());
      JsonNode data = event.get("event").get("data");
      assertThat(data.get("content").asText()).isEqualTo(payloadFile.toString());
      assertThat(data.get("type").asText()).isEqualTo("data/page");
    } finally {
      Files.deleteIfExists(templateFile);
      Files.deleteIfExists(templateSubDir);
    }
  }

  @Test
  void shouldResolveTemplateFromAbsolutePath(@TempDir Path tempDir) throws Exception {
    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html>hello</html>");

    Path templateFile = tempDir.resolve("absolute-template.json");
    Files.writeString(templateFile, createTemplateContent());

    registerTemplateAbsolute(templateFile);

    ProcessResult result = exec(
        "publish", "event",
        TEMPLATE_TYPE,
        payloadFile.toString(),
        "--output", "json"
    );

    result.assertSuccess();

    JsonNode event = MAPPER.readTree(result.stdout().strip());
    JsonNode data = event.get("event").get("data");
    assertThat(data.get("content").asText()).isEqualTo(payloadFile.toString());
    assertThat(data.get("type").asText()).isEqualTo("data/page");
  }

  @Test
  void shouldResolveDefaultTemplate(@TempDir Path tempDir) throws Exception {
    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html>hello</html>");

    ProcessResult result = exec(
        "publish", "event",
        DEFAULT_TEMPLATE_TYPE,
        payloadFile.toString(),
        "--output", "json"
    );

    result.assertSuccess();

    JsonNode event = MAPPER.readTree(result.stdout().strip());
    JsonNode data = event.get("event").get("data");
    assertThat(data).isNotNull();
  }

  @Test
  void shouldPreferSettingsTemplateWithRelativePathOverDefault(@TempDir Path tempDir)
      throws Exception {
    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html>hello</html>");

    Path templateFile = streamxHome.resolve("override-page-published.json");
    Files.writeString(templateFile, createTemplateContent());

    try {
      registerTemplate(DEFAULT_TEMPLATE_TYPE, "override-page-published.json");

      ProcessResult result = exec(
          "publish", "event",
          DEFAULT_TEMPLATE_TYPE,
          payloadFile.toString(),
          "--output", "json"
      );

      result.assertSuccess();

      JsonNode event = MAPPER.readTree(result.stdout().strip());
      JsonNode data = event.get("event").get("data");
      assertThat(data.get("content").asText()).isEqualTo(payloadFile.toString());
      assertThat(data.get("origin").asText()).isEqualTo("settings");
    } finally {
      unregisterTemplate(DEFAULT_TEMPLATE_TYPE);
      Files.deleteIfExists(templateFile);
    }
  }

  @Test
  void shouldPreferSettingsTemplateWithAbsolutePathOverDefault(@TempDir Path tempDir)
      throws Exception {
    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html>hello</html>");

    Path templateFile = tempDir.resolve("override-page-published.json");
    Files.writeString(templateFile, createTemplateContent());

    try {
      registerTemplateAbsolute(DEFAULT_TEMPLATE_TYPE, templateFile);

      ProcessResult result = exec(
          "publish", "event",
          DEFAULT_TEMPLATE_TYPE,
          payloadFile.toString(),
          "--output", "json"
      );

      result.assertSuccess();

      JsonNode event = MAPPER.readTree(result.stdout().strip());
      JsonNode data = event.get("event").get("data");
      assertThat(data.get("content").asText()).isEqualTo(payloadFile.toString());
      assertThat(data.get("origin").asText()).isEqualTo("settings");
    } finally {
      unregisterTemplate(DEFAULT_TEMPLATE_TYPE);
    }
  }

  @Test
  void shouldFailWhenRelativePathDoesNotExist(@TempDir Path tempDir) throws Exception {
    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html>hello</html>");

    registerTemplate("non-existent-template.json");

    ProcessResult result = exec(
        "publish", "event",
        TEMPLATE_TYPE,
        payloadFile.toString()
    );

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.eventTemplateNotFound(TEMPLATE_TYPE));
  }

  @Test
  void shouldFailWhenAbsolutePathDoesNotExist(@TempDir Path tempDir) throws Exception {
    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html>hello</html>");

    registerTemplateAbsolute(tempDir.resolve("non-existent-template.json"));

    ProcessResult result = exec(
        "publish", "event",
        TEMPLATE_TYPE,
        payloadFile.toString()
    );

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.eventTemplateNotFound(TEMPLATE_TYPE));
  }

  @Test
  void shouldFailWhenRelativePathResolvesToDirectory(@TempDir Path tempDir) throws Exception {
    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html>hello</html>");

    Path templateDir = streamxHome.resolve("template-dir");
    Files.createDirectories(templateDir);

    try {
      registerTemplate("template-dir");

      ProcessResult result = exec(
          "publish", "event",
          TEMPLATE_TYPE,
          payloadFile.toString()
      );

      result.assertExitCode(1);
      assertThat(result.stderr()).contains(msg.eventTemplateNotFound(TEMPLATE_TYPE));
    } finally {
      Files.deleteIfExists(templateDir);
    }
  }

  @Test
  void shouldFailWhenAbsolutePathIsDirectory(@TempDir Path tempDir) throws Exception {
    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html>hello</html>");

    registerTemplateAbsolute(tempDir);

    ProcessResult result = exec(
        "publish", "event",
        TEMPLATE_TYPE,
        payloadFile.toString()
    );

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.eventTemplateNotFound(TEMPLATE_TYPE));
  }
}