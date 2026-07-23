package com.streamx.cli.platform;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Organization(
    String id,
    String name,
    String projectsNumber,
    String role,
    String state
) {
  public static Organization fromJson(JsonNode node) {
    return new Organization(
        node.path("id").asText(null),
        node.path("name").asText(null),
        node.path("projectsNumber").asText(null),
        node.path("role").path("name").asText(null),
        node.path("state").asText(null)
    );
  }
}
