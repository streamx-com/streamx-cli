package com.streamx.cli.commands.local.run;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.MeshTestSupport;
import com.streamx.cli.test.annotation.DisabledIfDockerUnavailable;
import com.streamx.runner.docker.DockerClientFactory;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@DisabledIfDockerUnavailable
public class RunCommandIT extends CliBaseIT {

  private String meshPrefix;

  private static final String WEB_SERVER_SINK_IMAGE =
      "ghcr.io/streamx-com/streamx-blueprints/web-server-sink:3.0.7-jvm";

  private static final String BLOCKER_IMAGE = "alpine:3.20";

  @BeforeEach
  void isolateRunFromConcurrentInstances() {
    meshPrefix = "sx-run-" + UUID.randomUUID().toString().substring(0, 4) + "-";
    setEnv("streamx.container.startup-timeout-seconds", "180");
    setEnv("streamx.runner.pulsar.broker-port",
        String.valueOf(MeshTestSupport.freePort()));
    setEnv("streamx.runner.pulsar.http-port",
        String.valueOf(MeshTestSupport.freePort()));
    setEnv("test.proxy.host-port", String.valueOf(MeshTestSupport.freePort()));
  }

  @AfterEach
  void stopMeshAndResetRunnerState() {
    removeMeshContainers();
  }

  private int meshContainerCount() throws Exception {
    Process p = new ProcessBuilder("sh", "-c",
        "docker ps -aq --filter name=" + meshPrefix + " | wc -l").start();
    p.waitFor();
    return Integer.parseInt(new String(p.getInputStream().readAllBytes()).trim());
  }

  private void removeMeshContainers() {
    try {
      new ProcessBuilder("sh", "-c",
          "docker ps -aq --filter name=" + meshPrefix + " | xargs docker rm -f")
          .start().waitFor();
    } catch (Exception ignored) {
      // best-effort cleanup
    }
  }

  @Test
  void shouldWarnWhenEnvVariableIsUndefined() throws Exception {
    setEnv("streamx.runner.mesh-name-prefix", meshPrefix);
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
      if (handle.process().isAlive()) {
        handle.interruptAndJoin(Duration.ofSeconds(60).toMillis());
      }
    }
  }

  @Test
  void shouldFailWhenMeshFileDoesNotExist() throws Exception {
    String missing = streamxHome.resolve("no-such-mesh.yaml").toAbsolutePath().toString();

    ProcessResult result = exec("local", "run", "-f=" + missing);

    assertThat(result.exitCode()).isNotEqualTo(0);
    assertThat(result.stderr()).contains("Mesh file not found at: " + missing);
  }

  @Test
  void shouldReportContainerFailureWhenItsHostPortIsAlreadyTaken() throws Exception {
    setEnv("streamx.runner.mesh-name-prefix", meshPrefix);
    exec("settings", "set", "config.image.interpolated", WEB_SERVER_SINK_IMAGE);
    exec("settings", "set", "STREAMX_OWNER_SERVICE_NAME", meshPrefix + "test-owner");

    String meshPath = Paths.get("target/test-classes/mesh-interpolated.yaml")
        .toAbsolutePath()
        .normalize()
        .toString();

    int blockedPort = MeshTestSupport.freePort();
    String blockerId = startPortBlocker(blockedPort);
    setEnv("test.proxy.host-port", String.valueOf(blockedPort));

    AsyncProcessHandle handle = execAsync("local", "run", "-f=" + meshPath);
    try {
      Awaitility.await()
          .atMost(Duration.ofMinutes(3))
          .pollInterval(Duration.ofSeconds(1))
          .untilAsserted(() -> {
            assertThat(handle.getStdout())
                .as("the user must be told which container failed")
                .contains("rest-ingestion.proxy failed");
            assertThat(handle.getStderr())
                .as("the run must be reported as failed")
                .contains(msg.somethingWentWrong().strip());
          });
    } finally {
      if (handle.process().isAlive()) {
        handle.interruptAndJoin(Duration.ofSeconds(60).toMillis());
      }
      removePortBlocker(blockerId);
    }
  }

  private static String startPortBlocker(int hostPort) throws Exception {
    try (DockerClient docker = DockerClientFactory.create()) {
      docker.pullImageCmd(BLOCKER_IMAGE).start().awaitCompletion();
      ExposedPort containerPort = ExposedPort.tcp(80);
      Ports bindings = new Ports();
      bindings.bind(containerPort, Ports.Binding.bindPort(hostPort));
      String id = docker.createContainerCmd(BLOCKER_IMAGE)
          .withCmd("sleep", "300")
          .withExposedPorts(containerPort)
          .withHostConfig(HostConfig.newHostConfig().withPortBindings(bindings))
          .exec()
          .getId();
      docker.startContainerCmd(id).exec();
      return id;
    }
  }

  private static void removePortBlocker(String containerId) {
    try (DockerClient docker = DockerClientFactory.create()) {
      docker.removeContainerCmd(containerId).withForce(true).exec();
    } catch (Exception ignored) {
      // best-effort cleanup
    }
  }

  @Test
  void shouldFailWhenSystemPropertyIsUndefined() throws Exception {
    setEnv("streamx.runner.mesh-name-prefix", meshPrefix);
    exec("settings", "unset", "config.image.interpolated");
    exec("settings", "set", "STREAMX_OWNER_SERVICE_NAME", meshPrefix + "test-owner");

    String meshPath = Paths.get("target/test-classes/mesh-interpolated.yaml")
        .toAbsolutePath()
        .normalize()
        .toString();

    AsyncProcessHandle handle = execAsync("local", "run", "-f=" + meshPath);

    try {
      Awaitility.await()
          .atMost(Duration.ofMinutes(3))
          .pollInterval(Duration.ofSeconds(1))
          .until(() -> !handle.process().isAlive());

      ProcessResult result = handle.toResult();
      assertThat(result.exitCode()).isNotEqualTo(0);
      assertThat(result.stderr())
          .contains("Property 'config.image.interpolated'")
          .contains("is not set");
    } finally {
      if (handle.process().isAlive()) {
        handle.interruptAndJoin(Duration.ofSeconds(60).toMillis());
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
      if (handle.process().isAlive()) {
        handle.interruptAndJoin(Duration.ofSeconds(60).toMillis());
      }
    }
  }

  @Test
  void shouldSucceedWhenInterpolationValuesAreDefined() throws Exception {
    setEnv("streamx.runner.mesh-name-prefix", meshPrefix);
    exec("settings", "set", "config.image.interpolated", WEB_SERVER_SINK_IMAGE);
    exec("settings", "set", "STREAMX_OWNER_SERVICE_NAME", meshPrefix + "test-owner");

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
      assertThat(handle.process().isAlive()).isTrue();

      handle.interruptAndJoin(Duration.ofSeconds(60).toMillis());
      assertThat(handle.process().isAlive()).isFalse();

      ProcessResult result = handle.toResult();
      result.assertGracefulStop();
      Awaitility.await()
          .atMost(Duration.ofSeconds(30))
          .pollInterval(Duration.ofSeconds(1))
          .until(() -> meshContainerCount() == 0);
      assertThat(result.stderr()).doesNotContain("Exception");
    } finally {
      if (handle.process().isAlive()) {
        handle.interruptAndJoin(Duration.ofSeconds(60).toMillis());
      }
    }
  }

  @Test
  void shouldStartMeshSecondTimeAfterPreviousStopped() throws Exception {
    setEnv("streamx.runner.mesh-name-prefix", meshPrefix);
    exec("settings", "set", "config.image.interpolated", WEB_SERVER_SINK_IMAGE);
    exec("settings", "set", "STREAMX_OWNER_SERVICE_NAME", meshPrefix + "test-owner");

    String meshPath = Paths.get("target/test-classes/mesh-interpolated.yaml")
        .toAbsolutePath()
        .normalize()
        .toString();

    runUntilReadyThenStop(meshPath);
    // The second run reuses this test's prefix and ports: clear any straggling containers
    // rather than racing the first run's shutdown.
    removeMeshContainers();
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
      if (handle.process().isAlive()) {
        handle.interruptAndJoin(Duration.ofSeconds(60).toMillis());
      }
    }
  }

}
