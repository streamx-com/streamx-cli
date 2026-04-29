package com.streamx.cli.commands.settings.eventtemplates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

public final class EventTemplatesTestSupport {

  public static final ObjectMapper JSON = new ObjectMapper();
  public static final YAMLMapper YAML = new YAMLMapper();

  private EventTemplatesTestSupport() {
  }

  public static String sampleTemplate(String type) {
    return """
        {
          "specversion": "1.0",
          "id": "${uuid}",
          "source": "test-source",
          "type": "%s",
          "datacontenttype": "application/json",
          "subject": "${subject}",
          "time": "${currentTime}",
          "data": {}
        }
        """.formatted(type);
  }

  public static JsonNode findById(JsonNode templatesArray, String id) {
    for (JsonNode entry : templatesArray) {
      if (id.equals(entry.get("id").asText())) {
        return entry;
      }
    }
    return null;
  }
}
