package com.streamx.cli.commands.local.run;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.annotation.DisabledIfDockerUnavailable;
import io.quarkus.test.junit.QuarkusTest;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

@QuarkusTest
@DisabledIfDockerUnavailable
public class RunCommandIT extends CliBaseIT {

  private static final String PREFIX =
      "sx-run-" + UUID.randomUUID().toString().substring(0, 4) + "-";

  @Test
  void shouldRunStreamxExampleMesh() throws Exception {
    System.setProperty("streamx.runner.mesh-name-prefix", PREFIX);
    String meshPath = Paths.get("target/test-classes/mesh.yaml")
        .toAbsolutePath()
        .normalize()
        .toString();

    AsyncProcessHandle handle = execAsync("local", "run", "-f=" + meshPath);

    try {
      Awaitility.await()
          .atMost(Duration.ofMinutes(10))
          .pollInterval(Duration.ofSeconds(1))
          .until(() -> handle.getStdout().contains("STREAMX IS READY!"));

      Thread.sleep(Duration.ofSeconds(5));
      assertThat(handle.thread().isAlive()).isTrue();

      handle.interruptAndJoin(Duration.ofSeconds(30).toMillis());
      assertThat(handle.thread().isAlive()).isFalse();

      ProcessResult result = handle.toResult();
      result.assertSuccess();
    } finally {
      if (handle.thread().isAlive()) {
        handle.interruptAndJoin(Duration.ofSeconds(30).toMillis());
      }
    }
  }
}
