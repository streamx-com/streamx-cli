package com.streamx.cli.nativeimage;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.streamx.cli.test.annotation.DisabledIfDockerUnavailable;
import com.streamx.runner.docker.DockerClientFactory;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@DisabledIfDockerUnavailable
public class DockerNamedVolumeMountMetadataIT {

  private static final String IMAGE = "alpine:3.20";

  private DockerClient docker;
  private String containerId;
  private String volumeName;

  @AfterEach
  void cleanup() {
    if (containerId != null) {
      try {
        docker.removeContainerCmd(containerId).withForce(true).exec();
      } catch (Exception ignored) {
        // best-effort cleanup
      }
    }
    if (volumeName != null) {
      try {
        docker.removeVolumeCmd(volumeName).exec();
      } catch (Exception ignored) {
        // best-effort cleanup
      }
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
  void shouldExposeNameFieldOnInspectedNamedVolumeMount() throws Exception {
    docker = DockerClientFactory.create();
    docker.pullImageCmd(IMAGE).start().awaitCompletion();

    volumeName = "sx-test-vol-" + UUID.randomUUID().toString().substring(0, 8);
    docker.createVolumeCmd().withName(volumeName).exec();

    containerId = docker.createContainerCmd(IMAGE)
        .withCmd("sleep", "30")
        .withHostConfig(HostConfig.newHostConfig()
            .withBinds(new Bind(volumeName, new Volume("/data"))))
        .exec()
        .getId();
    docker.startContainerCmd(containerId).exec();

    InspectContainerResponse response = docker.inspectContainerCmd(containerId).exec();
    docker.listContainersCmd().withShowAll(true).exec();

    assertThat(response.getMounts())
        .extracting(InspectContainerResponse.Mount::getName)
        .contains(volumeName);
  }
}
