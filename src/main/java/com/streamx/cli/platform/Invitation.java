package com.streamx.cli.platform;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Invitation(
    String email,
    String role,
    String status
) {

  public static Invitation fromJson(JsonNode node) {
    return new Invitation(
        node.path("email").asText(null),
        node.path("role").path("name").asText(null),
        node.path("status").asText(null)
    );
  }
}
