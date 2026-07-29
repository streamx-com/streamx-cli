package com.streamx.cli.commands.local.run;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.streamx.cli.mesh.MeshManager;
import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.MeshTestSupport;
import com.streamx.cli.test.annotation.DisabledIfDockerUnavailable;
import com.streamx.runner.docker.DockerClientFactory;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@DisabledIfDockerUnavailable
public class RunCommandIT extends CliBaseIT {

  private static final String PREFIX =
      "sx-run-" + UUID.randomUUID().toString().substring(0, 4) + "-";

  private static final String WEB_SERVER_SINK_IMAGE =
      "ghcr.io/streamx-com/streamx-blueprints/web-server-sink:3.0.7-jvm";

  private static final String BLOCKER_IMAGE = "alpine:3.20";

  @BeforeEach
  void isolateRunFromConcurrentInstances() {
    System.setProperty("streamx.container.startup-timeout-seconds",
        MeshTestSupport.CONTAINER_STARTUP_TIMEOUT_SECONDS);
    System.setProperty("streamx.runner.pulsar.broker-port",
        String.valueOf(MeshTestSupport.freePort()));
    System.setProperty("streamx.runner.pulsar.http-port",
        String.valueOf(MeshTestSupport.freePort()));
    System.setProperty("test.proxy.host-port", String.valueOf(MeshTestSupport.freePort()));
  }

  @AfterEach
  void stopMeshAndResetRunnerState() {
    try {
      Arc.container().select(MeshManager.class).get().stop();
    } catch (Exception ignored) {
      // best-effort cleanup
    }
    System.clearProperty("streamx.runner.mesh-name-prefix");
    System.clearProperty("streamx.container.startup-timeout-seconds");
    System.clearProperty("streamx.runner.pulsar.broker-port");
    System.clearProperty("streamx.runner.pulsar.http-port");
    System.clearProperty("test.proxy.host-port");
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
  void shouldFailWhenMeshFileDoesNotExist() throws Exception {
    String missing = streamxHome.resolve("no-such-mesh.yaml").toAbsolutePath().toString();

    ProcessResult result = exec("local", "run", "-f=" + missing);

    assertThat(result.exitCode()).isNotEqualTo(0);
    assertThat(result.stderr()).contains("Mesh file not found at: " + missing);
  }

  @Test
  void shouldReportContainerFailureWhenItsHostPortIsAlreadyTaken() throws Exception {
    System.setProperty("streamx.runner.mesh-name-prefix", PREFIX);
    exec("settings", "set", "config.image.interpolated", WEB_SERVER_SINK_IMAGE);
    exec("settings", "set", "STREAMX_OWNER_SERVICE_NAME", PREFIX + "test-owner");

    String meshPath = Paths.get("target/test-classes/mesh-interpolated.yaml")
        .toAbsolutePath()
        .normalize()
        .toString();

    int blockedPort = MeshTestSupport.freePort();
    String blockerId = startPortBlocker(blockedPort);
    System.setProperty("test.proxy.host-port", String.valueOf(blockedPort));

    AsyncProcessHandle handle = execAsync("local", "run", "--verbose", "-f=" + meshPath);
    try {
      Awaitility.await()
          .atMost(Duration.ofMinutes(3))
          .pollInterval(Duration.ofSeconds(1))
          .untilAsserted(() -> assertThat(handle.getStderr())
              .as("the run must fail, and say it was host port %d that could not be bound",
                  blockedPort)
              .contains(msg.somethingWentWrong().strip())
              .contains(String.valueOf(blockedPort)));
    } finally {
      if (handle.thread().isAlive()) {
        handle.interruptAndJoin(Duration.ofSeconds(30).toMillis());
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
