package com.streamx.cli.platform;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Cluster(
    String id,
    String type,
    String name,
    boolean enabled,
    Double latitude,
    Double longitude
) {

  public static Cluster fromJson(JsonNode node, String type) {
    JsonNode location = node.path("location");
    return new Cluster(
        node.path("id").asText(null),
        type,
        node.path("name").asText(null),
        node.path("enabled").asBoolean(false),
        location.hasNonNull("latitude") ? location.path("latitude").asDouble() : null,
        location.hasNonNull("longitude") ? location.path("longitude").asDouble() : null
    );
  }
}
