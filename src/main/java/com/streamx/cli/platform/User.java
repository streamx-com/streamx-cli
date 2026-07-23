package com.streamx.cli.platform;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record User(
    String id,
    String displayName,
    String role,
    String status,
    boolean isCaller
) {

  public static final String ACTIVE = "ACTIVE";

  public boolean isActive() {
    return ACTIVE.equals(status);
  }

  public static User fromJson(JsonNode node) {
    return new User(
        node.path("id").asText(null),
        node.path("displayName").asText(null),
        node.path("role").path("name").asText(null),
        node.path("status").asText(null),
        node.path("isCaller").asBoolean(false)
    );
  }
}
