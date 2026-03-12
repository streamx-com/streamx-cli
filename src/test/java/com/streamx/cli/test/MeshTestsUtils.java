package com.streamx.cli.test;

import com.github.dockerjava.api.DockerClient;
import com.streamx.runner.docker.DockerClientFactory;
import com.streamx.runner.validation.DockerContainerValidator;
import java.util.Set;

public class MeshTestsUtils {
  public static void cleanUpMesh(String... containersToRemove) {
    DockerClient client = DockerClientFactory.create();

    for (String container : containersToRemove) {
      try {
        client.removeContainerCmd(container)
            .withForce(true)
            .exec();
      } catch (Exception ignored) {
        // Ignore
      }
    }
    new DockerContainerValidator().verifyExistingContainers(client, Set.of(containersToRemove));
  }
}
