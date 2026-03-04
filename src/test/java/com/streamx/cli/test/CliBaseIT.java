package com.streamx.cli.test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public abstract class CliBaseIT {

  private static final boolean NATIVE = Boolean.getBoolean("native.image");
  private static final Path TARGET = Path.of("target");
  private static final long DEFAULT_TIMEOUT_SECONDS = 30;

  private Process process;

  @BeforeEach
  void resetPublishedEventsBaseline() {
    MeshAssertions.resetPublishedEventsBaseline();
  }

  @BeforeAll
  static void ensureBuilt() {
    BuildExecutableOnce.ensureBuilt();
  }

  @AfterEach
  void cleanupProcess() {
    if (process != null && process.isAlive()) {
      process.destroyForcibly();
    }
  }

  protected ProcessResult execWithStdin(InputStream stdin, String... args) throws Exception {
    return execWithStdin(stdin, DEFAULT_TIMEOUT_SECONDS, args);
  }

  protected ProcessResult execWithStdin(String stdin, String... args) throws Exception {
    return execWithStdin(
        new java.io.ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)),
        args
    );
  }

  protected ProcessResult execWithStdin(
      InputStream stdin,
      long timeoutSeconds,
      String... args
  ) throws Exception {
    var command = new ArrayList<>(resolveBaseCommand());
    command.addAll(List.of(args));

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(false);
    process = pb.start();

    // Start capturing stdout/stderr BEFORE writing stdin
    StreamCapture stdoutCapture = captureAndForward(process.getInputStream(), System.out);
    StreamCapture stderrCapture = captureAndForward(process.getErrorStream(), System.err);

    // Write stdin on a separate thread to avoid deadlock
    Thread stdinWriter = Thread.ofVirtual().start(() -> {
      try (OutputStream os = process.getOutputStream()) {
        stdin.transferTo(os);
        os.flush();
      } catch (Exception ignored) {
        // Process may have exited early
      }
    });

    boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
    Assertions.assertTrue(finished, "Process timed out after %d seconds".formatted(timeoutSeconds));

    stdinWriter.join();
    String stdout = stdoutCapture.join();
    String stderr = stderrCapture.join();

    return new ProcessResult(process.exitValue(), stdout, stderr);
  }

  protected ProcessResult exec(String... args) throws Exception {
    return execWithStdin(InputStream.nullInputStream(), args);
  }

  private record StreamCapture(Thread thread, java.io.ByteArrayOutputStream buffer) {
    String join() throws InterruptedException {
      thread.join();
      return buffer.toString(StandardCharsets.UTF_8);
    }
  }

  private StreamCapture captureAndForward(InputStream source, java.io.PrintStream target) {
    var buffer = new java.io.ByteArrayOutputStream();
    Thread thread = Thread.ofVirtual().start(() -> {
      try {
        byte[] buf = new byte[1024];
        int len;
        while ((len = source.read(buf)) != -1) {
          buffer.write(buf, 0, len);
          target.write(buf, 0, len);
          target.flush();
        }
      } catch (Exception ignored) {
        // ignore
      }
    });
    return new StreamCapture(thread, buffer);
  }

  private static List<String> resolveBaseCommand() {
    if (NATIVE) {
      Path executable = findNativeExecutable();
      Assertions.assertTrue(Files.isExecutable(executable),
          "Native executable not found at %s. Run 'mvn package -Pnative -DskipTests' first"
              .formatted(executable));
      return List.of(executable.toAbsolutePath().toString());
    } else {
      Path jar = TARGET.resolve("quarkus-app/quarkus-run.jar");
      Assertions.assertTrue(jar.toFile().exists(),
          "JAR not found at %s. Run 'mvn package -DskipTests' first".formatted(jar));
      return List.of("java", "-jar", jar.toAbsolutePath().toString());
    }
  }

  private static Path findNativeExecutable() {
    try (var files = Files.list(TARGET)) {

      // TODO - check after native image build will be implemented
      return files
          .filter(p -> p.getFileName().toString().endsWith("-runner"))
          .filter(Files::isExecutable)
          .findFirst()
          .orElse(TARGET.resolve("*-runner"));
    } catch (Exception e) {
      return TARGET.resolve("*-runner");
    }
  }

  public record ProcessResult(int exitCode, String stdout, String stderr) {

    public void assertSuccess() {
      Assertions.assertEquals(0, exitCode,
          "Expected exit code 0 but got %d.\nSTDOUT: %s\nSTDERR: %s"
              .formatted(exitCode, stdout, stderr));
    }

    public void assertExitCode(int expected) {
      Assertions.assertEquals(expected, exitCode,
          "Expected exit code %d but got %d.\nSTDOUT: %s\nSTDERR: %s"
              .formatted(expected, exitCode, stdout, stderr));
    }
  }
}