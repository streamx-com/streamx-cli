package com.streamx.cli.commands.publish.event;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static com.streamx.cli.test.MeshAssertions.assertEventsPublished;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.profiles.DefaultMeshTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
@TestProfile(DefaultMeshTestProfile.class)
public class EventCommandIT extends CliBaseIT {

  @Test
  void shouldPublishEventWithKnownEventType(@TempDir Path tempDir) throws Exception {
    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html><body>Hello</body></html>");

    ProcessResult result = exec(
        "publish",
        "event",
        "com.streamx.blueprints.page.published.v1",
        payloadFile.toString()
    );

    result.assertSuccess();
    assertEventsPublished(1);
  }

  @Test
  void shouldPublishEventWithShortKnownEventType(@TempDir Path tempDir) throws Exception {
    Path payloadFile = tempDir.resolve("payload.html");
    Files.writeString(payloadFile, "<html><body>Hello</body></html>");

    ProcessResult result = exec(
        "publish",
        "event",
        "page.published.v1",
        payloadFile.toString()
    );

    result.assertSuccess();
    assertEventsPublished(1);
  }

  @Nested
  @QuarkusTest
  @TestProfile(DefaultMeshTestProfile.class)
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
  @TestProfile(DefaultMeshTestProfile.class)
  class InvalidPayload {

    @Test
    void shouldFailWhenPayloadFileNotFound() throws Exception {
      String nonExistentFile = "/tmp/non-existent-payload-file.html";

      ProcessResult result = exec(
          "publish",
          "event",
          "com.streamx.blueprints.page.published.v1",
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
          "com.streamx.blueprints.page.published.v1",
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
          "com.streamx.blueprints.page.published.v1",
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
  @TestProfile(DefaultMeshTestProfile.class)
  class VerboseOutput {

    @Test
    void shouldPrintVerboseOutputWhenFlagProvided(@TempDir Path tempDir) throws Exception {
      Path payloadFile = tempDir.resolve("payload.html");
      Files.writeString(payloadFile, "<html><body>Hello</body></html>");

      ProcessResult result = exec(
          "publish",
          "event",
          "com.streamx.blueprints.page.published.v1",
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
  @TestProfile(DefaultMeshTestProfile.class)
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

    private JsonNode getEventData(ProcessResult result) throws Exception {
      JsonNode event = MAPPER.readTree(result.stdout().strip());
      return event.get("event").get("data");
    }

    private JsonNode getEvent(ProcessResult result) throws Exception {
      return MAPPER.readTree(result.stdout().strip());
    }

    @Test
    void shouldSubstitutePayloadPathPlaceholder(@TempDir Path tempDir) throws Exception {
      Path payloadFile = tempDir.resolve("payload.html");
      Files.writeString(payloadFile, "<html>hello</html>");

      registerTemplate(tempDir,
          """
          {"content": "${payloadPath}", "type": "data/page"}""");

      ProcessResult result = exec(
          "publish", "event",
          CUSTOM_TEMPLATE_TYPE,
          payloadFile.toString(),
          "--output", "json"
      );

      result.assertSuccess();
      assertEventsPublished(1);

      JsonNode data = getEventData(result);
      assertThat(data.get("content").asText()).isEqualTo(payloadFile.toString());
      assertThat(data.get("type").asText()).isEqualTo("data/page");
    }

    @Test
    void shouldSubstituteFilePayloadPathWithBase64(@TempDir Path tempDir) throws Exception {
      Path payloadFile = tempDir.resolve("payload.html");
      String content = "<html>hello</html>";
      Files.writeString(payloadFile, content);

      String expectedBase64 = Base64.getEncoder().encodeToString(content.getBytes());

      registerTemplate(tempDir,
          """
          {"content": "file://${payloadPath}", "type": "data/page"}""");

      ProcessResult result = exec(
          "publish", "event",
          CUSTOM_TEMPLATE_TYPE,
          payloadFile.toString(),
          "--output", "json"
      );

      result.assertSuccess();
      assertEventsPublished(1);

      JsonNode data = getEventData(result);
      assertThat(data.get("content").asText()).isEqualTo(expectedBase64);
    }

    @Test
    void shouldSubstituteRelativePathPlaceholder(@TempDir Path tempDir) throws Exception {
      Path subDir = tempDir.resolve("a/b/c");
      Files.createDirectories(subDir);
      Path payloadFile = subDir.resolve("payload.html");
      Files.writeString(payloadFile, "<html>hello</html>");

      registerTemplate(tempDir,
          """
          {"content": "${relativePath}", "type": "data/page"}""");

      ProcessResult result = exec(
          "publish", "event",
          CUSTOM_TEMPLATE_TYPE,
          payloadFile.toString(),
          "--output", "json"
      );

      result.assertSuccess();
      assertEventsPublished(1);

      JsonNode data = getEventData(result);
      assertThat(data.get("content").asText()).isEqualTo(subDir.toString());
    }

    @Test
    void shouldSubstituteRelativePathPlaceholderWithLevel(@TempDir Path tempDir) throws Exception {
      Path subDir = tempDir.resolve("a/b/c");
      Files.createDirectories(subDir);
      Path payloadFile = subDir.resolve("payload.html");
      Files.writeString(payloadFile, "<html>hello</html>");

      registerTemplate(tempDir,
          """
          {"content": "${relativePath:2}", "type": "data/page"}""");

      ProcessResult result = exec(
          "publish", "event",
          CUSTOM_TEMPLATE_TYPE,
          payloadFile.toString(),
          "--output", "json"
      );

      result.assertSuccess();
      assertEventsPublished(1);

      JsonNode data = getEventData(result);
      assertThat(data.get("content").asText()).isEqualTo(tempDir.resolve("a").toString());
    }

    @Test
    void shouldSubstituteSubjectPlaceholderWithProvidedSubject(@TempDir Path tempDir)
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
          "custom/subject",
          "--output", "json"
      );

      result.assertSuccess();
      assertEventsPublished(1);

      JsonNode event = getEvent(result);
      assertThat(event.get("subject").asText()).isEqualTo("custom/subject");
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
    void shouldSubstituteMultiplePlaceholdersAcrossFields(@TempDir Path tempDir)
        throws Exception {
      Path subDir = tempDir.resolve("a/b");
      Files.createDirectories(subDir);
      Path payloadFile = subDir.resolve("payload.html");
      String content = "<html>hello</html>";
      Files.writeString(payloadFile, content);

      String expectedBase64 = Base64.getEncoder().encodeToString(content.getBytes());

      registerTemplate(tempDir,
          """
          {"content": "file://${payloadPath}", "path": "${relativePath:0}", "type": "data/page"}""");

      ProcessResult result = exec(
          "publish", "event",
          CUSTOM_TEMPLATE_TYPE,
          payloadFile.toString(),
          "custom/subject",
          "--output", "json"
      );

      result.assertSuccess();
      assertEventsPublished(1);

      JsonNode event = getEvent(result);
      assertThat(event.get("subject").asText()).isEqualTo("custom/subject");

      JsonNode data = getEventData(result);
      assertThat(data.get("content").asText()).isEqualTo(expectedBase64);
      assertThat(data.get("path").asText()).isEqualTo(subDir.toString());
      assertThat(data.get("type").asText()).isEqualTo("data/page");
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