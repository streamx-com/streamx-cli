package com.streamx.cli.commands.publish.stream;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static com.streamx.cli.test.MeshAssertions.assertEventsPublished;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.ingestion.CloudEvents;
import com.streamx.cli.ingestion.ConcatenatedJsonSerializer;
import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.CloudEventGenerator;
import com.streamx.cli.test.profiles.MeshTestProfile;
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
@TestProfile(MeshTestProfile.class)
public class StreamCommandIT extends CliBaseIT {
  CloudEventGenerator cloudEventGenerator = new CloudEventGenerator();
  ConcatenatedJsonSerializer jsonSerializer = new ConcatenatedJsonSerializer();

  @Test
  void shouldStreamEventsFromFilePath(@TempDir Path tempDir) throws Exception {
    List<CloudEvent> events = cloudEventGenerator.generate(5);
    List<JsonNode> eventsJson = events.stream().map(CloudEvents::toJson).toList();
    String eventsJsonString = jsonSerializer.serialize(eventsJson);

    Path eventsFile = tempDir.resolve("events");
    Files.writeString(eventsFile, eventsJsonString);

    ProcessResult result = exec(
        "publish",
        "stream",
        eventsFile.toString()
    );

    result.assertSuccess();

    assertEventsPublished(events.size());
    assertThat(result.stdout()).contains(msg.eventsPublished(events.size()));
  }


  @Test
  void shouldStreamEventsFromFileUri(@TempDir Path tempDir) throws Exception {
    List<CloudEvent> events = cloudEventGenerator.generate(5);
    List<JsonNode> eventsJson = events.stream().map(CloudEvents::toJson).toList();
    String eventsJsonString = jsonSerializer.serialize(eventsJson);

    Path eventsFile = tempDir.resolve("events");
    Files.writeString(eventsFile, eventsJsonString);

    ProcessResult result = exec(
        "publish",
        "stream",
        "file://" + eventsFile.toAbsolutePath()
    );

    result.assertSuccess();

    assertEventsPublished(events.size());
    assertThat(result.stdout()).contains(msg.eventsPublished(events.size()));
  }


  @Test
  void shouldStreamEventsFromHttpUri() throws Exception {
    List<CloudEvent> events = cloudEventGenerator.generate(5);
    List<JsonNode> eventsJson = events.stream().map(CloudEvents::toJson).toList();
    String eventsJsonString = jsonSerializer.serialize(eventsJson);

    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/events", exchange -> {
      byte[] responseBytes = eventsJsonString.getBytes();
      exchange.getResponseHeaders().set("Content-Type", "application/json");
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
      assertThat(result.stdout()).contains(msg.eventsPublished(events.size()));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void shouldStreamSingleEventFromStdin() throws Exception {
    String stdIn = CloudEvents.toJson(cloudEventGenerator.generate(1).getFirst()).toString();

    ProcessResult result = execWithStdin(stdIn, "publish", "stream");

    result.assertSuccess();

    assertEventsPublished(1);
    assertThat(result.stdout()).contains(msg.eventsPublished((1)));
  }

  @Test
  void shouldStreamManyEventsFromStdin() throws Exception {
    int eventsCount = 500;
    List<CloudEvent> events = cloudEventGenerator.generate(eventsCount);
    List<JsonNode> eventsJson = events.stream().map(CloudEvents::toJson).toList();
    String stdIn = jsonSerializer.serialize(eventsJson);

    ProcessResult result = execWithStdin(stdIn, "publish", "stream");
    result.assertSuccess();
    assertEventsPublished(eventsCount);
    assertThat(result.stdout()).contains(msg.eventsPublished((eventsCount)));
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
      List<JsonNode> eventsJson = events.stream().map(CloudEvents::toJson).toList();
      String eventsJsonString = jsonSerializer.serialize(eventsJson);

      Path unreadableFile = tempDir.resolve("unreadable.json");
      Files.writeString(unreadableFile, eventsJsonString);
      unreadableFile.toFile().setReadable(false);

      ProcessResult result = exec("publish", "stream", unreadableFile.toString());

      result.assertExitCode(1);
      assertThat(result.stderr()).contains(msg.sourceFileNotReadable(unreadableFile.toString()));
    }
  }
}