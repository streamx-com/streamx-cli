package com.streamx.cli.test.profiles;

import java.util.Map;

/**
 * Shared mesh configuration for parallel test execution.
 * Uses surefire fork number to isolate each fork's Docker containers and ports.
 */
final class MeshTestConfig {

  private MeshTestConfig() {
  }

  static Map<String, String> parallelMeshConfig() {
    String forkNumber = System.getProperty("surefire.forkNumber", "1");
    return Map.of(
        "streamx.runner.mesh-name-prefix", "sx-fork" + forkNumber + "-",
        "streamx.runner.pulsar.broker-port", "0",
        "streamx.runner.pulsar.http-port", "0",
        "streamx.runner.gateway.http-port", "0",
        "streamx.runner.gateway.admin-port", "0",
        "streamx.runner.ryuk.port", "0"
    );
  }
}
