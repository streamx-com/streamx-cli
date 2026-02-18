package com.streamx.cli.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.framework.CliException;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;

public class CloudEvents {
  public static CloudEvent fromJsonNode(JsonNode jsonNode) {
    ObjectMapper mapper = new ObjectMapper();
    EventFormat eventFormat = EventFormatProvider
        .getInstance()
        .resolveFormat(JsonFormat.CONTENT_TYPE);

    try {
      byte[] bytes = mapper.writeValueAsBytes(jsonNode);
      return eventFormat.deserialize(bytes);
    } catch (Exception e) {
      throw new CliException("CloudEvent deserialization failed: " + e.getMessage(), e);
    }
  }
}