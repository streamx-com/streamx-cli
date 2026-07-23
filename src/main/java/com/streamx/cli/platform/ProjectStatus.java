package com.streamx.cli.platform;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.ArrayList;
import java.util.List;

@RegisterForReflection
public record ProjectStatus(
    String state,
    List<Component> statuses
) {

  @RegisterForReflection
  public record Component(
      String state,
      String reason,
      String message
  ) {
  }

  public static ProjectStatus fromJson(JsonNode node) {
    List<Component> components = new ArrayList<>();
    for (JsonNode status : node.path("statuses")) {
      components.add(new Component(
          status.path("state").asText(null),
          status.path("reason").asText(null),
          status.path("message").asText(null)));
    }
    return new ProjectStatus(node.path("state").asText(null), components);
  }
}
