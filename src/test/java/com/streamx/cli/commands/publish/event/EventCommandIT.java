package com.streamx.cli.commands.publish.event;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static com.streamx.cli.test.MeshAssertions.assertEventsPublished;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.MeshAssertions;
import com.streamx.cli.test.MeshTestSupport;
import com.streamx.cli.test.annotation.DisabledIfDockerUnavailable;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
@DisabledIfDockerUnavailable
public class EventCommandIT extends CliBaseIT {

  @BeforeAll
  static void startMesh() {
    MeshTestSupport.startMesh("target/test-classes/mesh.yaml");
  }

  @AfterAll
  static void stopMesh() {
    MeshTestSupport.stopMesh();
  }

  @BeforeEach
  void resetBaseline() {
    MeshAssertions.resetPublishedEventsBaseline();
  }

  @Test
  void shouldPublishEventWithKnownEventTypes(@TempDir Path tempDir) throws Exception {
    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html><body>Hello</body></html>");

    URI uri = EventCommandIT.class.getResource("/default-event-templates").toURI();
    Path templatesDir = Path.of(uri);

    try (Stream<Path> files = Files.list(templatesDir)) {
      List<String> eventTypes = files
          .filter(p -> p.toString().endsWith(".json"))
          .map(p -> p.getFileName().toString().replace(".json", ""))
          .toList();

      for (String eventType : eventTypes) {
        System.out.println("Testing well known event type: " + eventType);
        ProcessResult result = exec(
            "publish",
            "event",
            eventType,
            payloadFile.toString()
        );

        result.assertSuccess();
        assertEventsPublished(1);
      }
    }
  }

  @Nested
  @QuarkusTest
  class InvalidTemplate {

    @Test
    void shouldFailWhenTemplateTypeNotFound(@TempDir Path tempDir) throws Exception {
      Path payloadFile = tempDir.resolve("payload.html");
      Files.writeString(payloadFile, "<html>hello</html>");

      ProcessResult result = exec(
          "publish",
          "event",
          "non.existent.template.type",
          payloadFile.toString()
      );

      result.assertExitCode(1);
      assertEventsPublished(0);
      assertThat(result.stderr()).contains(
          msg.eventTemplateNotFound("non.existent.template.type")
      );
    }
  }

  @Nested
  @QuarkusTest
  class InvalidPayload {

    @Test
    void shouldFailWhenPayloadFileNotFound() throws Exception {
      String nonExistentFile = "/tmp/non-existent-payload-file.html";

      ProcessResult result = exec(
          "publish",
          "event",
          "page.published",
          nonExistentFile
      );

      result.assertExitCode(1);
      assertEventsPublished(0);
      assertThat(result.stderr()).contains(msg.payloadFileNotFound(nonExistentFile));
    }

    @Test
    void shouldFailWhenPayloadIsDirectory(@TempDir Path tempDir) throws Exception {
      ProcessResult result = exec(
          "publish",
          "event",
          "page.published",
          tempDir.toString()
      );

      result.assertExitCode(1);
      assertEventsPublished(0);
      assertThat(result.stderr()).contains(msg.payloadFileIsDirectory(tempDir.toString()));
    }

    @Test
    void shouldFailWhenPayloadFileNotReadable(@TempDir Path tempDir) throws Exception {
      Path unreadableFile = tempDir.resolve("unreadable.html");
      Files.writeString(unreadableFile, "<html><body>Hello</body></html>");
      unreadableFile.toFile().setReadable(false);

      ProcessResult result = exec(
          "publish",
          "event",
          "page.published",
          unreadableFile.toString()
      );

      result.assertExitCode(1);
      assertEventsPublished(0);
      assertThat(result.stderr()).contains(
          msg.payloadFileNotReadable(unreadableFile.toString())
      );
    }
  }

  @Nested
  @QuarkusTest
  class VerboseOutput {

    @Test
    void shouldPrintVerboseOutputWhenFlagProvided(@TempDir Path tempDir) throws Exception {
      Path payloadFile = tempDir.resolve("payload.html");
      Files.writeString(payloadFile, "<html><body>Hello</body></html>");

      ProcessResult result = exec(
          "publish",
          "event",
          "page.published",
          payloadFile.toString(),
          "--verbose"
      );

      result.assertSuccess();
      assertEventsPublished(1);
      assertThat(result.stderr()).contains(msg.runningPublishEventCommand());
      assertThat(result.stderr()).contains(msg.resolvingStreamxClientConfig());
      assertThat(result.stderr()).contains(msg.initializingStreamxClient());
    }
  }

  @Nested
  @QuarkusTest
  class PlaceholderSubstitution {

    private static final String CUSTOM_TEMPLATE_TYPE = "custom.template";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private void registerTemplate(Path dir, String dataContent) throws Exception {
      String template = """
          {
            "specversion": "1.0",
            "id": "test-id",
            "source": "test-source",
            "type": "com.streamx.blueprints.page.published.v1",
            "datacontenttype": "application/json",
            "subject": "${subject}",
            "time": "2026-01-01T00:00:00.000000Z",
            "data": %s
          }
          """.formatted(dataContent);

      Path templateFile = dir.resolve("custom-template.json");
      Files.writeString(templateFile, template);

      exec("settings", "set",
          "eventtemplate." + CUSTOM_TEMPLATE_TYPE,
          templateFile.toString()
      ).assertSuccess();
    }

    private void registerFullTemplate(Path dir, String fullTemplate) throws Exception {
      Path templateFile = dir.resolve("custom-template.json");
      Files.writeString(templateFile, fullTemplate);

      exec("settings", "set",
          "eventtemplate." + CUSTOM_TEMPLATE_TYPE,
          templateFile.toString()
      ).assertSuccess();
    }

    private JsonNode getEventData(ProcessResult result) throws Exception {
      JsonNode event = MAPPER.readTree(result.stdout().strip());
      return event.get("event").get("data");
    }

    private JsonNode getEvent(ProcessResult result) throws Exception {
      return MAPPER.readTree(result.stdout().strip());
    }

    @Test
    void shouldSubstituteAllPlaceholders(@TempDir Path tempDir) throws Exception {
      Path subDir = tempDir.resolve("a/b/c");
      Files.createDirectories(subDir);
      Path payloadFile = subDir.resolve("payload.html");
      String content = "<html>hello</html>";
      Files.writeString(payloadFile, content);

      registerFullTemplate(tempDir, """
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
              "path": "${payloadPath}",
              "relativePath": "${relativePath}",
              "relativePathWithLevel": "${relativePath:2}",
              "type": "data/page"
            }
          }
          """);

      OffsetDateTime before = OffsetDateTime.now();

      ProcessResult result = exec(
          "publish", "event",
          CUSTOM_TEMPLATE_TYPE,
          payloadFile.toString(),
          "custom/subject",
          "--output", "json"
      );

      OffsetDateTime after = OffsetDateTime.now();

      result.assertSuccess();
      assertEventsPublished(1);

      JsonNode output = getEvent(result);
      JsonNode cloudEvent = output.get("event");

      // ${currentTime}
      String timeStr = cloudEvent.get("time").asText();
      OffsetDateTime eventTime =
          OffsetDateTime.parse(timeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
      assertThat(eventTime).isBetween(before, after);

      // ${uuid}
      assertThat(UUID.fromString(cloudEvent.get("id").asText())).isNotNull();

      // ${subject}
      assertThat(output.get("subject").asText()).isEqualTo("custom/subject");

      JsonNode data = getEventData(result);

      // file://${payloadPath}
      String expectedBase64 = Base64.getEncoder().encodeToString(content.getBytes());
      assertThat(data.get("content").asText()).isEqualTo(expectedBase64);

      // ${payloadPath}
      assertThat(data.get("path").asText()).isEqualTo(payloadFile.toString());

      // ${relativePath}
      assertThat(data.get("relativePath").asText()).isEqualTo("payload.html");

      // ${relativePath:2}
      assertThat(data.get("relativePathWithLevel").asText())
          .isEqualTo(Path.of("b", "c", "payload.html").toString());

      // non-placeholder value preserved
      assertThat(data.get("type").asText()).isEqualTo("data/page");
    }

    @Test
    void shouldSubstituteJsonPayloadPlaceholder(@TempDir Path tempDir) throws Exception {
      Path payloadFile = tempDir.resolve("payload.json");
      String content = "{\"title\":\"Hello World\"}";
      Files.writeString(payloadFile, content);

      registerTemplate(tempDir,
          """
              {"content": "json://${payloadPath}", "type": "data/page"}""");

      ProcessResult result = exec(
          "publish", "event",
          CUSTOM_TEMPLATE_TYPE,
          payloadFile.toString(),
          "--output", "json"
      );

      result.assertSuccess();
      assertEventsPublished(1);

      JsonNode data = getEventData(result);
      assertThat(data.get("content").asText()).isEqualTo(content);
      assertThat(data.get("type").asText()).isEqualTo("data/page");
    }

    @Test
    void shouldSubstituteFileRawPayloadPlaceholder(@TempDir Path tempDir) throws Exception {
      Path payloadFile = tempDir.resolve("payload.json");
      String content = "{\"title\":\"Hello World\"}";
      Files.writeString(payloadFile, content);

      registerTemplate(tempDir,
          """
              {"content": "file-raw://${payloadPath}", "type": "data/page"}""");

      ProcessResult result = exec(
          "publish", "event",
          CUSTOM_TEMPLATE_TYPE,
          payloadFile.toString(),
          "--output", "json"
      );

      result.assertSuccess();
      assertEventsPublished(1);

      JsonNode data = getEventData(result);
      assertThat(data.get("content").asText()).isEqualTo(content);
      assertThat(data.get("type").asText()).isEqualTo("data/page");
    }

    @Test
    void shouldFallbackToPayloadPathWhenSubjectNotProvided(@TempDir Path tempDir)
        throws Exception {
      Path payloadFile = tempDir.resolve("payload.html");
      Files.writeString(payloadFile, "<html>hello</html>");

      registerTemplate(tempDir,
          """
              {"content": "static", "type": "data/page"}""");

      ProcessResult result = exec(
          "publish", "event",
          CUSTOM_TEMPLATE_TYPE,
          payloadFile.toString(),
          "--output", "json"
      );

      result.assertSuccess();
      assertEventsPublished(1);

      JsonNode event = getEvent(result);
      assertThat(event.get("subject").asText()).isEqualTo(payloadFile.toString());
    }

    @Test
    void shouldHandleNestedObjectsWithPlaceholders(@TempDir Path tempDir) throws Exception {
      Path payloadFile = tempDir.resolve("payload.html");
      Files.writeString(payloadFile, "<html>hello</html>");

      registerTemplate(tempDir,
          """
              {"outer": {"inner": "${payloadPath}"}}""");

      ProcessResult result = exec(
          "publish", "event",
          CUSTOM_TEMPLATE_TYPE,
          payloadFile.toString(),
          "--output", "json"
      );

      result.assertSuccess();
      assertEventsPublished(1);

      JsonNode data = getEventData(result);
      assertThat(data.get("outer").get("inner").asText()).isEqualTo(payloadFile.toString());
    }

    @Test
    void shouldHandleArraysWithPlaceholders(@TempDir Path tempDir) throws Exception {
      Path payloadFile = tempDir.resolve("payload.html");
      Files.writeString(payloadFile, "<html>hello</html>");

      registerTemplate(tempDir,
          """
              {"items": ["${payloadPath}", "static", "${subject}"]}""");

      ProcessResult result = exec(
          "publish", "event",
          CUSTOM_TEMPLATE_TYPE,
          payloadFile.toString(),
          "my-subject",
          "--output", "json"
      );

      result.assertSuccess();
      assertEventsPublished(1);

      JsonNode data = getEventData(result);
      JsonNode items = data.get("items");
      assertThat(items.get(0).asText()).isEqualTo(payloadFile.toString());
      assertThat(items.get(1).asText()).isEqualTo("static");
      assertThat(items.get(2).asText()).isEqualTo("my-subject");
    }

    @Test
    void shouldPreserveNonPlaceholderValues(@TempDir Path tempDir) throws Exception {
      Path payloadFile = tempDir.resolve("payload.html");
      Files.writeString(payloadFile, "<html>hello</html>");

      registerTemplate(tempDir,
          """
              {"content": "static-value", "count": 42, "active": true}""");

      ProcessResult result = exec(
          "publish", "event",
          CUSTOM_TEMPLATE_TYPE,
          payloadFile.toString(),
          "--output", "json"
      );

      result.assertSuccess();
      assertEventsPublished(1);

      JsonNode data = getEventData(result);
      assertThat(data.get("content").asText()).isEqualTo("static-value");
      assertThat(data.get("count").asInt()).isEqualTo(42);
      assertThat(data.get("active").asBoolean()).isTrue();
    }
  }
}