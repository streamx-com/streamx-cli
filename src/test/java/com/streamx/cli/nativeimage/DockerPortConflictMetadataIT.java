package com.streamx.cli.nativeimage;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.streamx.cli.test.MeshTestSupport;
import com.streamx.cli.test.annotation.DisabledIfDockerUnavailable;
import com.streamx.runner.docker.DockerClientFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Triggers a Docker host-port bind conflict via docker-java directly so that the GraalVM
 * native-image tracing agent records the reflection metadata used in the port-conflict error path.
 */
@DisabledIfDockerUnavailable
public class DockerPortConflictMetadataIT {

  private static final String IMAGE = "alpine:3.20";

  private DockerClient docker;
  private String firstContainerId;
  private String secondContainerId;

  @AfterEach
  void cleanup() {
    if (firstContainerId != null) {
      tryRemove(firstContainerId);
    }
    if (secondContainerId != null) {
      tryRemove(secondContainerId);
    }
    if (docker != null) {
      try {
        docker.close();
      } catch (Exception ignored) {
        // best-effort
      }
    }
  }

  @Test
  void shouldThrowWhenStartingSecondContainerWithSameHostPort() throws Exception {
    docker = DockerClientFactory.create();
    docker.pullImageCmd(IMAGE).start().awaitCompletion();

    int hostPort = MeshTestSupport.freePort();
    ExposedPort containerPort = ExposedPort.tcp(80);
    Ports portBindings = new Ports();
    portBindings.bind(containerPort, Ports.Binding.bindPort(hostPort));

    firstContainerId = docker.createContainerCmd(IMAGE)
        .withCmd("sleep", "30")
        .withExposedPorts(containerPort)
        .withHostConfig(HostConfig.newHostConfig().withPortBindings(portBindings))
        .exec()
        .getId();
    docker.startContainerCmd(firstContainerId).exec();

    // Force Jackson to deserialize Container/ContainerPort/ContainerNetworkSettings.
    docker.listContainersCmd().withShowAll(true).exec();
    // Force Jackson to deserialize InspectContainerResponse.
    docker.inspectContainerCmd(firstContainerId).exec();

    secondContainerId = docker.createContainerCmd(IMAGE)
        .withCmd("sleep", "30")
        .withExposedPorts(containerPort)
        .withHostConfig(HostConfig.newHostConfig().withPortBindings(portBindings))
        .exec()
        .getId();

    assertThatThrownBy(() -> docker.startContainerCmd(secondContainerId).exec())
        .isInstanceOf(InternalServerErrorException.class);
  }

  private void tryRemove(String containerId) {
    try {
      docker.removeContainerCmd(containerId).withForce(true).exec();
    } catch (Exception ignored) {
      // best-effort cleanup
    }
  }
}
