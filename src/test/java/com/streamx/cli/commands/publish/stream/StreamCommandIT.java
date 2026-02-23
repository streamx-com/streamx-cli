package com.streamx.cli.commands.publish.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.ingestion.CloudEvents;
import com.streamx.cli.ingestion.ConcatenatedJsonSerializer;
import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.CloudEventGenerator;
import com.streamx.cli.test.MeshTestProfile;
import io.cloudevents.CloudEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

@QuarkusTest
@TestProfile(MeshTestProfile.class)
public class StreamCommandIT extends CliBaseIT {
  CloudEventGenerator cloudEventGenerator = new CloudEventGenerator();
  ConcatenatedJsonSerializer concatenatedJsonSerializer = new ConcatenatedJsonSerializer();

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
}