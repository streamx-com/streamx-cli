package com.streamx.cli.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.framework.CliException;
import io.cloudevents.CloudEvent;
import org.junit.jupiter.api.Test;

class CloudEventsSerdeTest {

  @Test
  void shouldDeserializeValidCloudEvent() throws Exception {
    JsonNode node = new ObjectMapper().readTree("""
        {
          "specversion": "1.0",
          "type": "com.example.test",
          "source": "/test",
          "id": "123"
        }
        """);

    CloudEvent event = CloudEventsSerde.fromJson(node);

    assertNotNull(event);
    assertEquals("1.0", event.getSpecVersion().toString());
    assertEquals("com.example.test", event.getType());
    assertEquals("/test", event.getSource().toString());
    assertEquals("123", event.getId());
  }

  @Test
  void shouldDeserializeCloudEventWithData() throws Exception {
    JsonNode node = new ObjectMapper().readTree("""
        {
          "specversion": "1.0",
          "type": "com.example.test",
          "source": "/test",
          "id": "456",
          "datacontenttype": "application/json",
          "data": { "key": "value" }
        }
        """);

    CloudEvent event = CloudEventsSerde.fromJson(node);

    assertNotNull(event);
    assertNotNull(event.getData());
    assertEquals("application/json", event.getDataContentType());
  }

  @Test
  void shouldThrowCliExceptionWhenRequiredFieldsAreMissing() throws Exception {
    JsonNode node = new ObjectMapper().readTree("""
        {
          "type": "com.example.test"
        }
        """);

    assertThrows(CliException.class, () -> CloudEventsSerde.fromJson(node));
  }

  @Test
  void shouldThrowCliExceptionWithMessageOnInvalidStructure() throws Exception {
    JsonNode node = new ObjectMapper().readTree("""
        {
          "invalid": "data"
        }
        """);

    CliException ex = assertThrows(CliException.class, () -> CloudEventsSerde.fromJson(node));
    assertTrue(ex.getMessage().startsWith("CloudEvent deserialization failed:"));
    assertNotNull(ex.getCause());
  }

  @Test
  void shouldThrowCliExceptionForEmptyObject() throws Exception {
    JsonNode node = new ObjectMapper().readTree("{}");

    assertThrows(CliException.class, () -> CloudEventsSerde.fromJson(node));
  }
}