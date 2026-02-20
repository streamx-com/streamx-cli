package com.streamx.cli.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.framework.CliException;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;

public class CloudEvents {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final EventFormat EVENT_FORMAT = EventFormatProvider
      .getInstance()
      .resolveFormat(JsonFormat.CONTENT_TYPE);

  public static CloudEvent fromJson(JsonNode jsonNode) {
    try {
      byte[] bytes = MAPPER.writeValueAsBytes(jsonNode);
      return EVENT_FORMAT.deserialize(bytes);
    } catch (Exception e) {
      throw new CliException("CloudEvent deserialization failed: " + e.getMessage(), e);
    }
  }

  public static JsonNode toJson(CloudEvent cloudEvent) {
    try {
      byte[] bytes = EVENT_FORMAT.serialize(cloudEvent);
      return MAPPER.readTree(bytes);
    } catch (Exception e) {
      throw new CliException("CloudEvent serialization failed: " + e.getMessage(), e);
    }
  }
}