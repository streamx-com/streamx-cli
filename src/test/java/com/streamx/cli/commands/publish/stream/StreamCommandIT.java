package com.streamx.cli.commands.publish.stream;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static com.streamx.cli.test.MeshAssertions.assertEventsPublished;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        msg.streamPublishingCompleted(events.size(), events.size(), 0, 0)
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
        msg.streamPublishingCompleted(events.size(), events.size(), 0, 0)
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
          msg.streamPublishingCompleted(events.size(), events.size(), 0, 0)
      );
    } finally {
      server.stop(0);
    }
  }

  @Test
  void shouldStreamSingleEventFromStdin() throws Exception {
    String input = CloudEventsSerde.toJson(cloudEventGenerator.generate(1).getFirst()).toString();

    ProcessResult result = execWithStdin(input, "publish", "stream");

    result.assertSuccess();

    assertEventsPublished(1);
    assertThat(result.stdout()).contains(
        msg.streamPublishingCompleted(1, 1, 0, 0)
    );
  }

  @Test
  void shouldStreamManyEventsFromStdin() throws Exception {
    int eventsCount = 500;
    List<CloudEvent> events = cloudEventGenerator.generate(eventsCount);
    List<JsonNode> eventsJson = events.stream().map(CloudEventsSerde::toJson).toList();
    String input = ConcatenatedJsonSerde.serialize(eventsJson);

    ProcessResult result = execWithStdin(input, "publish", "stream");
    result.assertSuccess();
    assertEventsPublished(eventsCount);
    assertThat(result.stdout()).contains(
        msg.streamPublishingCompleted(events.size(), events.size(), 0, 0)
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
        msg.streamPublishingCompleted(1, 0, 1, 0)
    );
  }



  @Test
  void shouldFormatOutputAsValidJsonIfErrorOccurredOrVerboseFlagIsSet() throws Exception {
    List<CloudEvent> validEvents = cloudEventGenerator.generate(3);
    String validBatch = ConcatenatedJsonSerde.serialize(
        validEvents.stream().map(CloudEventsSerde::toJson).toList()
    );
    String invalidBatch = """
        {
          "specversion": "1.0",
          "id": "id-1",
          "source": "streamx-commerce-accelerator",
          "type": "bad.type",
          "datacontenttype": "application/json",
          "subject": "subject-1",
          "time": "2026-01-01T00:00:00.000000Z",
          "data": {"content": "{}", "type": "data/category"}
        }
        {
          "specversion": "1.0",
          "id": "id-2",
          "source": "streamx-commerce-accelerator",
          "type": "bad.type",
          "datacontenttype": "application/json",
          "subject": "subject-2",
          "time": "2026-01-01T00:00:00.000000Z",
          "data": {"content": "{}", "type": "data/category"}
        }
        {
          "specversion": "1.0",
          "id": "id-3",
          "source": "streamx-commerce-accelerator",
          "type": "bad.type",
          "datacontenttype": "application/json",
          "subject": "subject-3",
          "time": "2026-01-01T00:00:00.000000Z",
          "data": {"content": "{}", "type": "data/category"}
        }
        """;

    String input = validBatch + invalidBatch;

    String expectedJson1 = """
          {
            "successCount" : 3,
            "failureCount" : 0,
            "unknownCount" : 3,
            "eventErrors" : [ {
              "eventNumber" : null,
              "batchNumber" : 2,
              "type" : "bad.type",
              "subject" : "subject-1",
              "errorMessage" : "Event publish result is unknown. Failed batch number: 2"
            }, {
              "eventNumber" : null,
              "batchNumber" : 2,
              "type" : "bad.type",
              "subject" : "subject-2",
              "errorMessage" : "Event publish result is unknown. Failed batch number: 2"
            }, {
              "eventNumber" : null,
              "batchNumber" : 2,
              "type" : "bad.type",
              "subject" : "subject-3",
              "errorMessage" : "Event publish result is unknown. Failed batch number: 2"
            } ],
            "batchSuccessCount" : 1,
            "batchFailureCount" : 1,
            "batchErrors" : [ {
              "batchNumber" : 2,
              "eventCount" : 3,
              "errorMessage" : "Bad request. Type [bad.type] is not allowed"
            } ]
          }
          """.strip();

    ProcessResult result1 = execWithStdin(
        input,
        "publish",
        "stream",
        "--batch-size",
        "3",
        "--output",
        "json",
        "--continue-on-error",
        "--verbose"
    );

    assertEquals(expectedJson1, result1.stdout().strip());

    String expectedJson2 = """
        {
          "successCount" : 3,
          "failureCount" : 0,
          "unknownCount" : 3,
          "eventErrors" : [ {
            "eventNumber" : null,
            "batchNumber" : 2,
            "type" : "bad.type",
            "subject" : "subject-1",
            "errorMessage" : "Event publish result is unknown. Failed batch number: 2"
          }, {
            "eventNumber" : null,
            "batchNumber" : 2,
            "type" : "bad.type",
            "subject" : "subject-2",
            "errorMessage" : "Event publish result is unknown. Failed batch number: 2"
          }, {
            "eventNumber" : null,
            "batchNumber" : 2,
            "type" : "bad.type",
            "subject" : "subject-3",
            "errorMessage" : "Event publish result is unknown. Failed batch number: 2"
          } ],
          "batchSuccessCount" : 1,
          "batchFailureCount" : 1,
          "batchErrors" : [ {
            "batchNumber" : 2,
            "eventCount" : 3,
            "errorMessage" : "Bad request. Type [bad.type] is not allowed"
          } ]
        }
        """.strip();

    ProcessResult result2 = execWithStdin(
        input,
        "publish",
        "stream",
        "--batch-size",
        "3",
        "--output",
        "json",
        "--verbose"
    );

    assertEquals(expectedJson2, result2.stdout().strip());
  }

  @Nested
  @QuarkusTest
  @TestProfile(DefaultMeshTestProfile.class)
  class BatchStreaming {

    @Test
    void shouldStreamEventsInBatches() throws Exception {
      int eventCount = 11;
      int batchSize = 3;
      List<CloudEvent> events = cloudEventGenerator.generate(eventCount);
      String input = ConcatenatedJsonSerde.serialize(
          events.stream().map(CloudEventsSerde::toJson).toList()
      );

      ProcessResult result = execWithStdin(
          input,
          "publish",
          "stream",
          "--batch-size",
          String.valueOf(batchSize)
      );

      result.assertSuccess();
      assertEventsPublished(eventCount);
      assertThat(result.stdout()).contains(
          msg.streamBatchPublishingCompleted(eventCount, eventCount, 0, 0, 4, 4, 0)
      );
    }

    @Test
    void shouldFailOnFirstFailedBatch() throws Exception {
      List<CloudEvent> validEvents = cloudEventGenerator.generate(3);
      String validBatch = ConcatenatedJsonSerde.serialize(
          validEvents.stream().map(CloudEventsSerde::toJson).toList()
      );

      String invalidBatch = """
          {
            "specversion": "1.0",
            "id": "id-1",
            "source": "streamx-commerce-accelerator",
            "type": "bad.type",
            "datacontenttype": "application/json",
            "subject": "subject-1",
            "time": "2026-01-01T00:00:00.000000Z",
            "data": {"content": "{}", "type": "data/category"}
          }
          {
            "specversion": "1.0",
            "id": "id-2",
            "source": "streamx-commerce-accelerator",
            "type": "bad.type",
            "datacontenttype": "application/json",
            "subject": "subject-2",
            "time": "2026-01-01T00:00:00.000000Z",
            "data": {"content": "{}", "type": "data/category"}
          }
          {
            "specversion": "1.0",
            "id": "id-3",
            "source": "streamx-commerce-accelerator",
            "type": "bad.type",
            "datacontenttype": "application/json",
            "subject": "subject-3",
            "time": "2026-01-01T00:00:00.000000Z",
            "data": {"content": "{}", "type": "data/category"}
          }
          """;

      String input = validBatch + invalidBatch;

      ProcessResult result = execWithStdin(
          input,
          "publish",
          "stream",
          "--batch-size",
          "3"
      );

      result.assertExitCode(1);
      assertEventsPublished(3);
      assertThat(result.stderr()).contains("Bad request. Type [bad.type] is not allowed");
      assertThat(result.stdout()).contains(
          msg.streamBatchPublishingCompleted(6, 3, 0, 3, 2, 1, 1)
      );
    }

    @Test
    void shouldContinueOnFailedBatchIfContinueOnErrorFlagProvided() throws Exception {
      List<CloudEvent> validEvents1 = cloudEventGenerator.generate(3);
      String validBatch1 = ConcatenatedJsonSerde.serialize(
          validEvents1.stream().map(CloudEventsSerde::toJson).toList()
      );

      String invalidBatch = """
          {
            "specversion": "1.0",
            "id": "id-1",
            "source": "streamx-commerce-accelerator",
            "type": "bad.type",
            "datacontenttype": "application/json",
            "subject": "subject-1",
            "time": "2026-01-01T00:00:00.000000Z",
            "data": {"content": "{}", "type": "data/category"}
          }
          {
            "specversion": "1.0",
            "id": "id-2",
            "source": "streamx-commerce-accelerator",
            "type": "bad.type",
            "datacontenttype": "application/json",
            "subject": "subject-2",
            "time": "2026-01-01T00:00:00.000000Z",
            "data": {"content": "{}", "type": "data/category"}
          }
          {
            "specversion": "1.0",
            "id": "id-3",
            "source": "streamx-commerce-accelerator",
            "type": "bad.type",
            "datacontenttype": "application/json",
            "subject": "subject-3",
            "time": "2026-01-01T00:00:00.000000Z",
            "data": {"content": "{}", "type": "data/category"}
          }
          """;

      List<CloudEvent> validEvents2 = cloudEventGenerator.generate(3);
      String validBatch2 = ConcatenatedJsonSerde.serialize(
          validEvents2.stream().map(CloudEventsSerde::toJson).toList()
      );

      String input = validBatch1 + invalidBatch + validBatch2;

      ProcessResult result = execWithStdin(
          input,
          "publish",
          "stream",
          "--batch-size",
          "3",
          "--continue-on-error"
      );

      result.assertExitCode(0);
      assertEventsPublished(6);
      assertThat(result.stderr()).contains("Bad request. Type [bad.type] is not allowed");
      assertThat(result.stdout()).contains(
          msg.streamBatchPublishingCompleted(9, 6, 0, 3, 3, 2, 1)
      );
    }
  }

  @Nested
  @QuarkusTest
  @TestProfile(DefaultMeshTestProfile.class)
  class ContinueOnError {
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

      assertThat(result.stdout()).contains(msg.streamPublishingCompleted(2, 1, 1, 0));
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

      assertThat(result.stdout()).contains(msg.streamPublishingCompleted(4, 2, 2, 0));
      assertThat(result.stderr()).contains("Bad request. Type [bad.type] is not allowed");
      assertThat(result.stderr()).contains("Bad request. Type [ugly.type] is not allowed");
    }

    @Test
    void shouldFailOnFirstFailedBatch() throws Exception {
      int batchSize = 100;

      String invalidFirstBatch = ConcatenatedJsonSerde.serialize(
          CloudEventGenerator.builder().type("bad.type").build().generate(batchSize)
              .stream().map(CloudEventsSerde::toJson).toList()
      );
      String remainingValidEvents = ConcatenatedJsonSerde.serialize(
          cloudEventGenerator.generate(1050).stream().map(CloudEventsSerde::toJson).toList()
      );

      String input = invalidFirstBatch + remainingValidEvents;

      ProcessResult result = execWithStdin(input, "publish", "stream",
          "--batch-size", String.valueOf(batchSize));

      result.assertExitCode(1);
      assertEventsPublished(0);
      assertThat(result.stderr()).contains("Bad request. Type [bad.type] is not allowed");
      assertThat(result.stdout()).contains(
          msg.streamBatchPublishingCompleted(100, 0, 0, 100, 1, 0, 1)
      );
    }

    @Test
    void shouldContinueOnFailedBatchIfContinueOnErrorFlagProvided() throws Exception {
      int batchSize = 100;

      String validBatch1to3 = ConcatenatedJsonSerde.serialize(
          cloudEventGenerator.generate(300).stream().map(CloudEventsSerde::toJson).toList()
      );
      String invalidBatch4 = ConcatenatedJsonSerde.serialize(
          CloudEventGenerator.builder().type("bad.type").build().generate(100)
              .stream().map(CloudEventsSerde::toJson).toList()
      );
      String validBatch5to7 = ConcatenatedJsonSerde.serialize(
          cloudEventGenerator.generate(300).stream().map(CloudEventsSerde::toJson).toList()
      );
      String invalidBatch8 = ConcatenatedJsonSerde.serialize(
          CloudEventGenerator.builder().type("ugly.type").build().generate(100)
              .stream().map(CloudEventsSerde::toJson).toList()
      );
      String validBatch9to12 = ConcatenatedJsonSerde.serialize(
          cloudEventGenerator.generate(350).stream().map(CloudEventsSerde::toJson).toList()
      );

      String input =
          validBatch1to3 + invalidBatch4 + validBatch5to7 + invalidBatch8 + validBatch9to12;

      ProcessResult result = execWithStdin(
          input,
          "publish",
          "stream",
          "--batch-size",
          String.valueOf(batchSize),
          "--continue-on-error");

      result.assertExitCode(0);
      assertEventsPublished(950);
      assertThat(result.stderr()).contains("Bad request. Type [bad.type] is not allowed");
      assertThat(result.stderr()).contains("Bad request. Type [ugly.type] is not allowed");
      assertThat(result.stdout()).contains(
          msg.streamBatchPublishingCompleted(1150, 950, 0, 200, 12, 10, 2)
      );
    }
  }

  @Nested
  @QuarkusTest
  @TestProfile(DefaultMeshTestProfile.class)
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
        assertThat(result.stderr()).contains(msg.unableToOpenSourceInputStream(
            uri,
            "HTTP 404 Not Found"
        ));
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
        assertThat(result.stderr())
            .contains(msg.unableToOpenSourceInputStream(
                uri,
                "HTTP 500 Internal Server Error"
            ));
      } finally {
        server.stop(0);
      }
    }

    @Test
    void shouldFailWhenHttpUriNotReachable() throws Exception {
      String uri = "http://localhost:1/unreachable";

      ProcessResult result = exec("publish", "stream", uri);

      result.assertExitCode(1);
      assertThat(result.stderr())
          .contains(msg.unableToOpenSourceInputStream(uri, msg.connectionRefused()));
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
