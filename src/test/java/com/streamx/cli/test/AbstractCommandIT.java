package com.streamx.cli.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class AbstractCommandIT {

  private static final boolean NATIVE = Boolean.getBoolean("native.image");
  private static final Path TARGET = Path.of("target");
  private static final long DEFAULT_TIMEOUT_SECONDS = 30;

  private Process process;

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

  protected ProcessResult execWithStdin(InputStream stdin, long timeoutSeconds, String... args) throws Exception {
    var command = new ArrayList<>(resolveBaseCommand());
    command.addAll(List.of(args));

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(false);
    process = pb.start();

    try (OutputStream os = process.getOutputStream()) {
      stdin.transferTo(os);
      os.flush();
    }

    boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
    Assertions.assertTrue(finished, "Process timed out after %d seconds".formatted(timeoutSeconds));

    String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

    return new ProcessResult(process.exitValue(), stdout, stderr);
  }

  protected ProcessResult exec(String... args) throws Exception {
    return execWithStdin(InputStream.nullInputStream(), args);
  }

  private static List<String> resolveBaseCommand() {
    if (NATIVE) {
      Path executable = findNativeExecutable();
      Assertions.assertTrue(Files.isExecutable(executable),
          "Native executable not found at %s. Run 'mvn package -Pnative -DskipTests' first."
              .formatted(executable));
      return List.of(executable.toAbsolutePath().toString());
    } else {
      Path jar = TARGET.resolve("quarkus-app/quarkus-run.jar");
      Assertions.assertTrue(jar.toFile().exists(),
          "JAR not found at %s. Run 'mvn package -DskipTests' first.".formatted(jar));
      return List.of("java", "-jar", jar.toAbsolutePath().toString());
    }
  }

  private static Path findNativeExecutable() {
    try (var files = Files.list(TARGET)) {
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