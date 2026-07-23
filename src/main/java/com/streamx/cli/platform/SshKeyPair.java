package com.streamx.cli.platform;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SshKeyPair(String privateKey, String publicKey) {

  public static SshKeyPair fromJson(JsonNode node) {
    return new SshKeyPair(
        node.path("privateKey").asText(null),
        node.path("publicKey").asText(null)
    );
  }
}
