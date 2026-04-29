package com.streamx.cli.commands.local.run;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.mesh.MeshManager;
import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.annotation.DisabledIfDockerUnavailable;
import com.streamx.runner.event.ContainerFailed;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.event.Observes;
import java.net.ServerSocket;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

@QuarkusTest
@DisabledIfDockerUnavailable
public class RunCommandIT extends CliBaseIT {

  private static final String PREFIX =
      "sx-run-" + UUID.randomUUID().toString().substring(0, 4) + "-";

  private static final String WEB_SERVER_SINK_IMAGE =
      "ghcr.io/streamx-com/streamx-blueprints/web-server-sink:3.0.7-jvm";

  private static final AtomicInteger CONTAINER_FAILED_COUNT = new AtomicInteger(0);

  void onContainerFailed(@Observes ContainerFailed event) {
    CONTAINER_FAILED_COUNT.incrementAndGet();
  }

  @Test
  void shouldWarnWhenEnvVariableIsUndefined() throws Exception {
    System.setProperty("streamx.runner.mesh-name-prefix", PREFIX);
    exec("settings", "set", "config.image.interpolated", WEB_SERVER_SINK_IMAGE);
    exec("settings", "unset", "STREAMX_OWNER_SERVICE_NAME");
    clearEnv("STREAMX_OWNER_SERVICE_NAME");

    String meshPath = Paths.get("target/test-classes/mesh-interpolated.yaml")
        .toAbsolutePath()
        .normalize()
        .toString();

    AsyncProcessHandle handle = execAsync("local", "run", "-f=" + meshPath);

    try {
      Awaitility.await()
          .atMost(Duration.ofMinutes(3))
          .pollInterval(Duration.ofSeconds(1))
          .until(() -> handle.getStderr()
              .contains("Environment variable 'STREAMX_OWNER_SERVICE_NAME'"));

      assertThat(handle.getStderr())
          .contains("WARNING:")
          .contains("STREAMX_OWNER_SERVICE_NAME");
    } finally {
      if (handle.thread().isAlive()) {
        handle.interruptAndJoin(Duration.ofSeconds(30).toMillis());
      }
    }
  }

  @Test
  void shouldFailWhenRequiredPortIsAlreadyAllocated() throws Exception {
    System.setProperty("streamx.runner.mesh-name-prefix", PREFIX);
    exec("settings", "set", "config.image.interpolated", WEB_SERVER_SINK_IMAGE);
    exec("settings", "set", "STREAMX_OWNER_SERVICE_NAME", PREFIX + "test-owner");

    String meshPath = Paths.get("target/test-classes/mesh-interpolated.yaml")
        .toAbsolutePath()
        .normalize()
        .toString();

    int failureBaseline = CONTAINER_FAILED_COUNT.get();

    try (ServerSocket blocker = new ServerSocket(0)) {
      int blockedPort = blocker.getLocalPort();
      System.setProperty("streamx.runner.gateway.http-port", String.valueOf(blockedPort));

      AsyncProcessHandle handle = execAsync("local", "run", "-f=" + meshPath);

      try {
        Awaitility.await()
            .atMost(Duration.ofMinutes(3))
            .pollInterval(Duration.ofSeconds(1))
            .until(() -> CONTAINER_FAILED_COUNT.get() > failureBaseline);

        assertThat(CONTAINER_FAILED_COUNT.get()).isGreaterThan(failureBaseline);
      } finally {
        if (handle.thread().isAlive()) {
          handle.interruptAndJoin(Duration.ofSeconds(30).toMillis());
        }
        try {
          Arc.container().select(MeshManager.class).get().stop();
        } catch (Exception ignored) {
          // best-effort cleanup
        }
        System.clearProperty("streamx.runner.gateway.http-port");
      }
    }
  }

  @Test
  void shouldFailWhenSystemPropertyIsUndefined() throws Exception {
    System.setProperty("streamx.runner.mesh-name-prefix", PREFIX);
    exec("settings", "unset", "config.image.interpolated");
    exec("settings", "set", "STREAMX_OWNER_SERVICE_NAME", PREFIX + "test-owner");

    String meshPath = Paths.get("target/test-classes/mesh-interpolated.yaml")
        .toAbsolutePath()
        .normalize()
        .toString();

    AsyncProcessHandle handle = execAsync("local", "run", "-f=" + meshPath);

    try {
      Awaitility.await()
          .atMost(Duration.ofMinutes(3))
          .pollInterval(Duration.ofSeconds(1))
          .until(() -> !handle.thread().isAlive());

      ProcessResult result = handle.toResult();
      assertThat(result.exitCode()).isNotEqualTo(0);
      assertThat(result.stderr())
          .contains("Property 'config.image.interpolated'")
          .contains("is not set");
    } finally {
      if (handle.thread().isAlive()) {
        handle.interruptAndJoin(Duration.ofSeconds(30).toMillis());
      }
    }
  }

  /**
   * Verifies that {@link com.streamx.cli.config.StreamxHome#bridgeConfigToSystemProperties()}
   * propagates a runner-related setting from the streamxHome config file to a JVM system
   * property, where the streamx-service-mesh runner picks it up via {@code ConfigProvider}.
   *
   * <p>The setup intentionally uses ONLY {@code exec("settings", "set", ...)} (no
   * {@code System.setProperty}) to write the runner key. If the bridge works the runner
   * will use that prefix when naming its containers and the test passes; if it doesn't,
   * the runner falls back to the default {@code sx-} prefix.
   */
  @Test
  void shouldBridgeRunnerSettingToSystemPropertyForLocalRun() throws Exception {
    String bridgedPrefix = "sx-bridge-" + UUID.randomUUID().toString().substring(0, 4) + "-";
    System.clearProperty("streamx.runner.mesh-name-prefix");

    exec("settings", "set", "streamx.runner.mesh-name-prefix", bridgedPrefix).assertSuccess();
    exec("settings", "set", "config.image.interpolated", WEB_SERVER_SINK_IMAGE).assertSuccess();
    exec("settings", "set", "STREAMX_OWNER_SERVICE_NAME",
        bridgedPrefix + "test-owner").assertSuccess();

    String meshPath = Paths.get("target/test-classes/mesh-interpolated.yaml")
        .toAbsolutePath()
        .normalize()
        .toString();

    AsyncProcessHandle handle = execAsync("local", "run", "-f=" + meshPath);

    try {
      Awaitility.await()
          .atMost(Duration.ofMinutes(3))
          .pollInterval(Duration.ofSeconds(1))
          .until(() -> handle.getStdout().contains("STREAMX IS READY!"));

      assertThat(handle.getStdout())
          .as("runner should use the prefix from streamxHome settings via the bridge")
          .contains(bridgedPrefix);
    } finally {
      if (handle.thread().isAlive()) {
        handle.interruptAndJoin(Duration.ofSeconds(30).toMillis());
      }
      System.clearProperty("streamx.runner.mesh-name-prefix");
    }
  }

  @Test
  void shouldSucceedWhenInterpolationValuesAreDefined() throws Exception {
    System.setProperty("streamx.runner.mesh-name-prefix", PREFIX);
    exec("settings", "set", "config.image.interpolated", WEB_SERVER_SINK_IMAGE);
    exec("settings", "set", "STREAMX_OWNER_SERVICE_NAME", PREFIX + "test-owner");

    String meshPath = Paths.get("target/test-classes/mesh-interpolated.yaml")
        .toAbsolutePath()
        .normalize()
        .toString();

    AsyncProcessHandle handle = execAsync("local", "run", "-f=" + meshPath);

    try {
      Awaitility.await()
          .atMost(Duration.ofMinutes(3))
          .pollInterval(Duration.ofSeconds(1))
          .until(() -> handle.getStdout().contains("STREAMX IS READY!"));

      Thread.sleep(Duration.ofSeconds(5));
      assertThat(handle.thread().isAlive()).isTrue();

      handle.interruptAndJoin(Duration.ofSeconds(30).toMillis());
      assertThat(handle.thread().isAlive()).isFalse();

      ProcessResult result = handle.toResult();
      result.assertSuccess();
      assertThat(result.stdout()).contains("Stopping mesh...");
      assertThat(result.stderr()).doesNotContain("Exception");
    } finally {
      if (handle.thread().isAlive()) {
        handle.interruptAndJoin(Duration.ofSeconds(30).toMillis());
      }
    }
  }

  @Test
  void shouldStartMeshSecondTimeAfterPreviousStopped() throws Exception {
    System.setProperty("streamx.runner.mesh-name-prefix", PREFIX);
    exec("settings", "set", "config.image.interpolated", WEB_SERVER_SINK_IMAGE);
    exec("settings", "set", "STREAMX_OWNER_SERVICE_NAME", PREFIX + "test-owner");

    String meshPath = Paths.get("target/test-classes/mesh-interpolated.yaml")
        .toAbsolutePath()
        .normalize()
        .toString();

    runUntilReadyThenStop(meshPath);
    runUntilReadyThenStop(meshPath);
  }

  private void runUntilReadyThenStop(String meshPath) throws InterruptedException {
    AsyncProcessHandle handle = execAsync("local", "run", "-f=" + meshPath);
    try {
      Awaitility.await()
          .atMost(Duration.ofMinutes(3))
          .pollInterval(Duration.ofSeconds(1))
          .until(() -> handle.getStdout().contains("STREAMX IS READY!"));

      assertThat(handle.getStderr())
          .doesNotContain("MissingReflectionRegistrationError");
    } finally {
      if (handle.thread().isAlive()) {
        handle.interruptAndJoin(Duration.ofSeconds(30).toMillis());
      }
    }
  }

}
