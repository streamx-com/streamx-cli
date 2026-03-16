package com.streamx.cli.commands.publish.stream;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static com.streamx.cli.test.MeshAssertions.assertEventsPublished;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.ingestion.CloudEventsSerde;
import com.streamx.cli.ingestion.ConcatenatedJsonSerde;
import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.CloudEventGenerator;
import com.streamx.cli.test.MeshTestEnv;
import com.streamx.cli.test.annotation.DisabledIfDockerUnavailable;
import com.streamx.cli.test.profiles.MeshWithAuthTestProfile;
import io.cloudevents.CloudEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
@DisabledIfDockerUnavailable
@TestProfile(MeshWithAuthTestProfile.class)
public class StreamCommandIngestionConfigIT extends CliBaseIT {
  CloudEventGenerator cloudEventGenerator = new CloudEventGenerator();

  @Inject
  MeshTestEnv meshTestEnv;

  @Test
  void shouldFailInNoAuthTokenProvided() throws Exception {
    List<CloudEvent> events = cloudEventGenerator.generate(5);
    List<JsonNode> eventsJson = events.stream().map(CloudEventsSerde::toJson).toList();
    String eventsJsonString = ConcatenatedJsonSerde.serialize(eventsJson);

    ProcessResult result = execWithStdin(
        eventsJsonString,
        "publish",
        "stream"
    );

    result.assertExitCode(1);
    assertEventsPublished(0);

    assertThat(result.stderr()).contains("Event publish failed (1):");
    assertThat(result.stderr()).contains("Authentication failed");

    assertThat(result.stdout())
        .contains(msg.streamPublishingCompleted(1, 0, 1, 0));
  }

  @Test
  void shouldSucceedIfAuthTokenProvided() throws Exception {
    List<CloudEvent> events = cloudEventGenerator.generate(5);
    List<JsonNode> eventsJson = events.stream().map(CloudEventsSerde::toJson).toList();
    String eventsJsonString = ConcatenatedJsonSerde.serialize(eventsJson);

    ProcessResult result = execWithStdin(
        eventsJsonString,
        "publish",
        "stream",
        "--auth-token",
        meshTestEnv.awaitAuthToken()
    );

    result.assertSuccess();
    assertEventsPublished(5);

    assertThat(result.stdout())
        .contains(msg.streamPublishingCompleted(5, 5, 0, 0));
  }

  @Test
  void shouldFailIfInvalidIngestionUrlProvided() throws Exception {
    List<CloudEvent> events = cloudEventGenerator.generate(5);
    List<JsonNode> eventsJson = events.stream().map(CloudEventsSerde::toJson).toList();
    String eventsJsonString = ConcatenatedJsonSerde.serialize(eventsJson);

    ProcessResult result = execWithStdin(
        eventsJsonString,
        "publish",
        "stream",
        "--auth-token",
        meshTestEnv.awaitAuthToken(),
        "--ingestion-url",
        "http://localhost:4242"
    );

    result.assertExitCode(1);
    assertEventsPublished(0);

    assertThat(result.stderr()).contains("Event publish failed (1):");
    assertThat(result.stderr()).contains(
        "POST request with URI: "
            + "http://localhost:4242/ingestion/v2/cloudevents failed due to HTTP client error"
    );

    assertThat(result.stdout())
        .contains(msg.streamPublishingCompleted(1, 0, 1, 0));
  }

  @Test
  void shouldSucceedIfValidIngestionUrlProvided() throws Exception {
    List<CloudEvent> events = cloudEventGenerator.generate(5);
    List<JsonNode> eventsJson = events.stream().map(CloudEventsSerde::toJson).toList();
    String eventsJsonString = ConcatenatedJsonSerde.serialize(eventsJson);

    ProcessResult result = execWithStdin(
        eventsJsonString,
        "publish",
        "stream",
        "--auth-token",
        meshTestEnv.awaitAuthToken(),
        "--ingestion-url",
        "http://localhost:8080"
    );

    result.assertSuccess();
    assertEventsPublished(5);

    assertThat(result.stdout())
        .contains(msg.streamPublishingCompleted(5, 5, 0, 0));
  }
}