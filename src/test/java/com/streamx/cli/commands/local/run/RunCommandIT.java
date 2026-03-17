package com.streamx.cli.commands.local.run;

import static com.streamx.cli.test.MeshTestsUtils.cleanUpMesh;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.annotation.DisabledIfDockerUnavailable;
import io.quarkus.test.junit.QuarkusTest;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@DisabledIfDockerUnavailable
public class RunCommandIT extends CliBaseIT {

  @AfterEach
  void awaitDockerResourcesAreRemoved() {
    Awaitility.await()
        .atMost(Duration.ofMinutes(2))
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
  void shouldRunStreamxExampleMesh() throws Exception {
    String meshPath = Paths.get("target/test-classes/mesh.yaml")
        .toAbsolutePath()
        .normalize()
        .toString();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();

    InputStream originalIn = System.in;
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;

    AtomicInteger exitCode = new AtomicInteger(-1);

    System.setIn(InputStream.nullInputStream());
    System.setOut(new PrintStream(out, true));
    System.setErr(new PrintStream(err, true));

    Thread commandThread = Thread.ofVirtual().start(() -> {
      exitCode.set(createCommandLine().execute("local", "run", "-f=" + meshPath));
    });

    try {
      Awaitility.await()
          .atMost(Duration.ofMinutes(10))
          .pollInterval(Duration.ofSeconds(1))
          .until(() -> out.toString(StandardCharsets.UTF_8).contains("STREAMX IS READY!"));

      Thread.sleep(Duration.ofSeconds(5));

      assertThat(commandThread.isAlive())
          .as("Mesh process should still be running after STREAMX IS READY!")
          .isTrue();

      assertThat(out.toString(StandardCharsets.UTF_8)).contains("STREAMX IS READY!");
    } finally {
      System.setIn(originalIn);
      System.setOut(originalOut);
      System.setErr(originalErr);

      commandThread.interrupt();
      commandThread.join(Duration.ofSeconds(30).toMillis());
    }
  }
}