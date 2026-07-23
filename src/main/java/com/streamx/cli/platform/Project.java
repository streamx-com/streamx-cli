package com.streamx.cli.platform;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Project(
    String id,
    String name,
    String description,
    String state
) {

  public static Project fromJson(JsonNode node) {
    return new Project(
        node.path("id").asText(null),
        node.path("name").asText(null),
        node.path("description").asText(null),
        node.path("state").asText(null)
    );
  }
}
