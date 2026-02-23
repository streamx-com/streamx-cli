package com.streamx.cli.commands.publish.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.ingestion.CloudEvents;
import com.streamx.cli.ingestion.ConcatenatedJsonSerializer;
import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.CloudEventGenerator;
import com.streamx.cli.test.MeshTestProfile;
import com.sun.net.httpserver.HttpServer;
import io.cloudevents.CloudEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@QuarkusTest
@TestProfile(MeshTestProfile.class)
public class StreamCommandIT extends CliBaseIT {
  CloudEventGenerator cloudEventGenerator = new CloudEventGenerator();
  ConcatenatedJsonSerializer concatenatedJsonSerializer = new ConcatenatedJsonSerializer();

  String eventsJson = """
        {
          "specversion": "1.0",
          "id": "Accent Furniture",
          "source": "streamx-commerce-accelerator",
          "type": "com.streamx.blueprints.data.published.v1",
          "datacontenttype": "application/json",
          "subject": "cat:Accent Furniture",
          "time": "2026-01-01T00:00:00.000000Z",
          "data": {
            "content": "{\\"id\\":\\"Accent Furniture\\",\\"slug\\":\\"accent-furniture\\",\\"name\\":\\"Accent Furniture\\"}",
            "type": "data/category"
          }
        }
        {
          "specversion": "1.0",
          "id": "Bar & Serving Carts",
          "source": "streamx-commerce-accelerator",
          "type": "com.streamx.blueprints.data.published.v1",
          "datacontenttype": "application/json",
          "subject": "cat:Bar & Serving Carts",
          "time": "2026-01-01T00:00:00.000000Z",
          "data": {
            "content": "{\\"id\\":\\"Bar & Serving Carts\\",\\"slug\\":\\"bar-&-serving-carts\\",\\"name\\":\\"Bar & Serving Carts\\"}",
            "type": "data/category"
          }
        }
        """;

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

  @Test
  void shouldStreamEventsFromFilePath(@TempDir Path tempDir) throws Exception {
    Path eventsFile = tempDir.resolve("events");
    Files.writeString(eventsFile, eventsJson);

    ProcessResult result = exec(
        "publish",
        "stream",
        eventsFile.toString()
    );

    result.assertSuccess();
  }


  @Test
  void shouldStreamEventsFromFileUri(@TempDir Path tempDir) throws Exception {
    Path eventsFile = tempDir.resolve("events");
    Files.writeString(eventsFile, eventsJson);

    ProcessResult result = exec(
        "publish",
        "stream",
        "file://" + eventsFile.toAbsolutePath()
    );

    result.assertSuccess();
  }


  @Test
  void shouldStreamEventsFromHttpUri() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/events", exchange -> {
      byte[] responseBytes = eventsJson.getBytes();
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
    } finally {
      server.stop(0);
    }
  }

  @Test
  void shouldStreamSingleEventFromStdin() throws Exception {
    String stdIn = CloudEvents.toJson(cloudEventGenerator.generate(1).getFirst()).toString();

    ProcessResult result = execWithStdin(stdIn, "publish", "stream");

    result.assertSuccess();
  }

  @Test
  void shouldStreamManyEventsFromStdin() throws Exception {
    List<CloudEvent> events = cloudEventGenerator.generate(500);
    List<JsonNode> eventsJson = events.stream().map(CloudEvents::toJson).toList();
    String stdIn = concatenatedJsonSerializer.serialize(eventsJson);

    ProcessResult result = execWithStdin(stdIn, "publish", "stream");
    result.assertSuccess();
  }

  @Test
  void shouldFailOnInvalidJson() throws Exception {
    ProcessResult result = execWithStdin(invalidEventsJson, "publish", "stream");

    result.assertExitCode(1);
  }
}