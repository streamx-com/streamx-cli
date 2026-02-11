package com.streamx.cli.commands.local.run;

import static com.streamx.cli.commands.util.MeshTestsUtils.cleanUpMesh;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.commands.util.MeshStopper;
import com.streamx.runner.event.MeshStarted;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.nio.file.Paths;
import java.util.Map;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusMainTest
@TestProfile(RunCommandTest.RunCommandProfile.class)
public class RunCommandTest {

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
    String s = Paths.get("target/test-classes/mesh.yaml")
        .toAbsolutePath()
        .normalize()
        .toString();
    LaunchResult result = launcher.launch("local", "run", "-f=" + s);

    assertThat(result.getOutput()).contains("STREAMX IS READY!");
  }

  @ApplicationScoped
  @IfBuildProperty(name = "streamx.run.test.profile", stringValue = "true")
  public static class Listener {

    @Inject
    MeshStopper meshStopper;

    void onMeshStarted(@Observes MeshStarted event) {
      meshStopper.scheduleStop();
    }
  }

  public static class RunCommandProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of("streamx.run.test.profile", "true");
    }
  }
}
