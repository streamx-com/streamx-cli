package com.streamx.cli.platform;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.ArrayList;
import java.util.List;

@RegisterForReflection
public record ProjectRepository(
    String name,
    String uri,
    String branch,
    String commitId,
    Boolean ready,
    List<String> errorMessages,
    boolean sshKeyProvided
) {

  public static ProjectRepository fromJson(JsonNode node) {
    JsonNode status = node.path("projectRepositoryStatus");
    List<String> errors = new ArrayList<>();
    for (JsonNode error : status.path("errorMessages")) {
      errors.add(error.asText());
    }
    return new ProjectRepository(
        node.path("name").asText(null),
        node.path("uri").asText(null),
        node.path("branch").asText(null),
        node.path("commitId").asText(null),
        status.path("ready").isMissingNode() ? null : status.path("ready").asBoolean(),
        errors,
        node.path("sshKeyProvided").asBoolean(false)
    );
  }
}
