package com.streamx.cli.commands.local.run;

import static com.streamx.cli.test.MeshTestsUtils.cleanUpMesh;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import java.nio.file.Paths;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusMainTest
public class RunCommandTest extends CliBaseIT {

  @AfterEach
  void awaitDockerResourcesAreRemoved() {
    Awaitility.await()
        .until(() -> {
          try {
            cleanUpMesh(
                "pulsar", "pulsar-init",
                "local-service-mesh-proxy", "rest-ingestion.proxy",
                "pages-relay.service", "web-server-sink.sink");
            return true;
          } catch (Exception e) {
            return false;
          }
        });
  }

  @Test
  void shouldRunStreamxExampleMesh(QuarkusMainLauncher launcher) {
    String meshPath = Paths.get("target/test-classes/mesh.yaml")
        .toAbsolutePath()
        .normalize()
        .toString();
    LaunchResult result = launcher.launch("local", "run", "-f=" + meshPath);

    assertThat(result.getOutput()).contains("STREAMX IS READY!");
  }
}
