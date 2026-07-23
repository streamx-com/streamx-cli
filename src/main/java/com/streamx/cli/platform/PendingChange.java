package com.streamx.cli.platform;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.ArrayList;
import java.util.List;

@RegisterForReflection
public record PendingChange(
    String message,
    List<String> details
) {

  public static PendingChange fromJson(JsonNode node) {
    List<String> details = new ArrayList<>();
    for (JsonNode detail : node.path("details")) {
      details.add(detail.asText());
    }
    return new PendingChange(node.path("message").asText(null), details);
  }
}
