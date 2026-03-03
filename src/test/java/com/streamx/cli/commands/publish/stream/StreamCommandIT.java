package com.streamx.cli.commands.publish.stream;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static com.streamx.cli.test.MeshAssertions.assertEventsPublished;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.ingestion.CloudEventsSerde;
import com.streamx.cli.ingestion.ConcatenatedJsonSerde;
import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.CloudEventGenerator;
import com.streamx.cli.test.profiles.DefaultMeshTestProfile;
import com.sun.net.httpserver.HttpServer;
import io.cloudevents.CloudEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
@TestProfile(DefaultMeshTestProfile.class)
public class StreamCommandIT extends CliBaseIT {
  CloudEventGenerator cloudEventGenerator = new CloudEventGenerator();

  @Test
  void shouldStreamEventsFromFilePath(@TempDir Path tempDir) throws Exception {
    List<CloudEvent> events = cloudEventGenerator.generate(5);
    List<JsonNode> eventsJson = events.stream().map(CloudEventsSerde::toJson).toList();
    String eventsJsonString = ConcatenatedJsonSerde.serialize(eventsJson);

    Path eventsFile = tempDir.resolve("events");
    Files.writeString(eventsFile, eventsJsonString);

    ProcessResult result = exec(
        "publish",
        "stream",
        eventsFile.toString()
    );

    result.assertSuccess();

    assertEventsPublished(events.size());
    assertThat(result.stdout()).contains(
        msg.streamPublishingCompleted(events.size(), events.size(), 0)
    );
  }


  @Test
  void shouldStreamEventsFromFileUri(@TempDir Path tempDir) throws Exception {
    List<CloudEvent> events = cloudEventGenerator.generate(5);
    List<JsonNode> eventsJson = events.stream().map(CloudEventsSerde::toJson).toList();
    String eventsJsonString = ConcatenatedJsonSerde.serialize(eventsJson);

    Path eventsFile = tempDir.resolve("events");
    Files.writeString(eventsFile, eventsJsonString);

    ProcessResult result = exec(
        "publish",
        "stream",
        "file://" + eventsFile.toAbsolutePath()
    );

    result.assertSuccess();

    assertEventsPublished(events.size());
    assertThat(result.stdout()).contains(
        msg.streamPublishingCompleted(events.size(), events.size(), 0)
    );
  }


  @Test
  void shouldStreamEventsFromHttpUri() throws Exception {
    List<CloudEvent> events = cloudEventGenerator.generate(5);
    List<JsonNode> eventsJson = events.stream().map(CloudEventsSerde::toJson).toList();
    String eventsJsonString = ConcatenatedJsonSerde.serialize(eventsJson);

    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/events", exchange -> {
      byte[] responseBytes = eventsJsonString.getBytes();
      exchange.sendResponseHeaders(200, responseBytes.length);
      try (var outputStream = exchange.getResponseBody()) {
        outputStream.write(responseBytes);
      }
    });
    server.start();

    try {
      int port = server.getAddress().getPort();
      String uri = "http://localhost:" + port + "/events";

      ProcessResult result = exec("publish", "stream", uri);

      result.assertSuccess();

      assertEventsPublished(events.size());
      assertThat(result.stdout()).contains(
          msg.streamPublishingCompleted(events.size(), events.size(), 0)
      );
    } finally {
      server.stop(0);
    }
  }

  @Test
  void shouldStreamSingleEventFromStdin() throws Exception {
    String stdIn = CloudEventsSerde.toJson(cloudEventGenerator.generate(1).getFirst()).toString();

    ProcessResult result = execWithStdin(stdIn, "publish", "stream");

    result.assertSuccess();

    assertEventsPublished(1);
    assertThat(result.stdout()).contains(
        msg.streamPublishingCompleted(1, 1, 0)
    );
  }

  @Test
  void shouldStreamManyEventsFromStdin() throws Exception {
    int eventsCount = 500;
    List<CloudEvent> events = cloudEventGenerator.generate(eventsCount);
    List<JsonNode> eventsJson = events.stream().map(CloudEventsSerde::toJson).toList();
    String stdIn = ConcatenatedJsonSerde.serialize(eventsJson);

    ProcessResult result = execWithStdin(stdIn, "publish", "stream");
    result.assertSuccess();
    assertEventsPublished(eventsCount);
    assertThat(result.stdout()).contains(
        msg.streamPublishingCompleted(events.size(), events.size(), 0)
    );
  }

  @Test
  void shouldFailOnInvalidJson() throws Exception {
    String invalidEventsJson = """
        { // invalid json
          specversion: "1.0",
          "id": "Accent Furniture",
          "source": "streamx-commerce-accelerator",
          "type": "com.streamx.blueprints.data.published.v1",
          "datacontenttype": "application/json",
          "subject": "cat:Accent Furniture",
          "time": "2026-01-01T00:00:00.000000Z",
          "data": {
        """;

    ProcessResult result = execWithStdin(invalidEventsJson, "publish", "stream");

    result.assertExitCode(1);
    assertEventsPublished(0);

    assertThat(result.stderr()).contains("Failed to parse JSON: Unexpected character");
    assertThat(result.stdout()).contains(
        msg.streamPublishingCompleted(1, 0, 1)
    );
  }

  @Test
  void shouldFailOnFirstInvalidEvent() throws Exception {
    String validEvent1 = ConcatenatedJsonSerde.serialize(
        cloudEventGenerator.generate(1).stream().map(CloudEventsSerde::toJson).toList()
    );

    String invalidEvents = """
        {
          "specversion": "1.0",
          "id": "Accent Furniture",
          "source": "streamx-commerce-accelerator",
          "type": "bad.type",
          "datacontenttype": "application/json",
          "subject": "${relativePath}",
          "time": "2026-01-01T00:00:00.000000Z",
          "data": {
            "content": "{}",
            "type": "data/category"
          }
        }
        {
          "specversion": "1.0",
          "id": "Accent Furniture",
          "source": "streamx-commerce-accelerator",
          "type": "ugly.type",
          "datacontenttype": "application/json",
          "subject": "${relativePath}",
          "time": "2026-01-01T00:00:00.000000Z",
          "data": {
            "content": "{}",
            "type": "data/category"
          }
        }
        """;

    String validEvent2 = ConcatenatedJsonSerde.serialize(
        cloudEventGenerator.generate(1).stream().map(CloudEventsSerde::toJson).toList()
    );

    String input = validEvent1 + invalidEvents + validEvent2;

    ProcessResult result = execWithStdin(input, "publish", "stream");

    result.assertExitCode(1);
    assertEventsPublished(1);

    assertThat(result.stdout()).contains(msg.streamPublishingCompleted(2, 1, 1));
    assertThat(result.stderr()).contains("Bad request. Type [bad.type] is not allowed");
  }

  @Test
  void shouldContinueOnInvalidEventsIfContinueOnErrorFlagProvided() throws Exception {
    String validEvent1 = ConcatenatedJsonSerde.serialize(
        cloudEventGenerator.generate(1).stream().map(CloudEventsSerde::toJson).toList()
    );

    String invalidEvents = """
        {
          "specversion": "1.0",
          "id": "Accent Furniture",
          "source": "streamx-commerce-accelerator",
          "type": "bad.type",
          "datacontenttype": "application/json",
          "subject": "${relativePath}",
          "time": "2026-01-01T00:00:00.000000Z",
          "data": {
            "content": "{}",
            "type": "data/category"
          }
        }
        {
          "specversion": "1.0",
          "id": "Accent Furniture",
          "source": "streamx-commerce-accelerator",
          "type": "ugly.type",
          "datacontenttype": "application/json",
          "subject": "${relativePath}",
          "time": "2026-01-01T00:00:00.000000Z",
          "data": {
            "content": "{}",
            "type": "data/category"
          }
        }
        """;

    String validEvent2 = ConcatenatedJsonSerde.serialize(
        cloudEventGenerator.generate(1).stream().map(CloudEventsSerde::toJson).toList()
    );

    String input = validEvent1 + invalidEvents + validEvent2;

    ProcessResult result = execWithStdin(input, "publish", "stream", "--continue-on-error");

    result.assertExitCode(0);
    assertEventsPublished(2);

    assertThat(result.stdout()).contains(msg.streamPublishingCompleted(4, 2, 2));
    assertThat(result.stderr()).contains("Bad request. Type [bad.type] is not allowed");
    assertThat(result.stderr()).contains("Bad request. Type [ugly.type] is not allowed");
  }


  @Nested
  class InvalidSource {
    @Test
    void shouldFailWhenFileNotFound() throws Exception {
      String nonExistentFile = "/tmp/non-existent-events-file.json";

      ProcessResult result = exec("publish", "stream", nonExistentFile);

      result.assertExitCode(1);
      assertThat(result.stderr()).contains(msg.sourceFileNotFound(nonExistentFile));
    }

    @Test
    void shouldFailWhenSourceIsDirectory(@TempDir Path tempDir) throws Exception {
      ProcessResult result = exec("publish", "stream", tempDir.toString());

      result.assertExitCode(1);
      assertThat(result.stderr()).contains(msg.sourceIsDirectory(tempDir.toString()));
    }

    @Test
    void shouldFailWhenFileUriNotFound() throws Exception {
      String nonExistentPath = "/tmp/non-existent-events-file.json";
      String fileUri = "file://" + nonExistentPath;

      ProcessResult result = exec("publish", "stream", fileUri);

      result.assertExitCode(1);
      assertThat(result.stderr()).contains(msg.sourceFileNotFound(nonExistentPath));
    }

    @Test
    void shouldFailWhenHttpUriReturns404() throws Exception {
      HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
      server.createContext("/missing", exchange -> {
        exchange.sendResponseHeaders(404, -1);
        exchange.close();
      });
      server.start();

      try {
        int port = server.getAddress().getPort();
        String uri = "http://localhost:" + port + "/missing";

        ProcessResult result = exec("publish", "stream", uri);

        result.assertExitCode(1);
        assertThat(result.stderr()).contains(msg.sourceUriNotFound(uri));
      } finally {
        server.stop(0);
      }
    }

    @Test
    void shouldFailWhenHttpUriReturns500() throws Exception {
      HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
      server.createContext("/error", exchange -> {
        exchange.sendResponseHeaders(500, -1);
        exchange.close();
      });
      server.start();

      try {
        int port = server.getAddress().getPort();
        String uri = "http://localhost:" + port + "/error";

        ProcessResult result = exec("publish", "stream", uri);

        result.assertExitCode(1);
        assertThat(result.stderr()).contains(msg.sourceUriNotAccessible(uri, "500"));
      } finally {
        server.stop(0);
      }
    }

    @Test
    void shouldFailWhenHttpUriNotReachable() throws Exception {
      String uri = "http://localhost:1/unreachable";

      ProcessResult result = exec("publish", "stream", uri);

      result.assertExitCode(1);
      assertThat(result.stderr()).contains(msg.sourceUriNotReachable(uri, "Connection refused"));
    }

    @Test
    void shouldFailWhenSourceFileNotReadable(@TempDir Path tempDir) throws Exception {
      List<CloudEvent> events = cloudEventGenerator.generate(5);
      List<JsonNode> eventsJson = events.stream().map(CloudEventsSerde::toJson).toList();
      String eventsJsonString = ConcatenatedJsonSerde.serialize(eventsJson);

      Path unreadableFile = tempDir.resolve("unreadable.json");
      Files.writeString(unreadableFile, eventsJsonString);
      unreadableFile.toFile().setReadable(false);

      ProcessResult result = exec("publish", "stream", unreadableFile.toString());

      result.assertExitCode(1);
      assertThat(result.stderr()).contains(msg.sourceFileNotReadable(unreadableFile.toString()));
    }
  }
}