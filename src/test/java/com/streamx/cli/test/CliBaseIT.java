package com.streamx.cli.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

public abstract class CliBaseIT {

  private static final long DEFAULT_TIMEOUT_SECONDS = 30;

  protected static final String CONFIG_FILE_PATH =
      "contexts/default/config/application.properties";

  @TempDir
  public Path streamxHome;

  private Process process;
  private final Map<String, String> envVars = new HashMap<>();

  protected Path getConfigPath() {
    return streamxHome.resolve(CONFIG_FILE_PATH);
  }

  @BeforeAll
  static void ensureBuilt() {
    CliArtifact.ensureBuilt();
  }

  protected void setEnv(String key, String value) {
    envVars.put(key, value);
  }

  protected void clearEnv(String key) {
    envVars.remove(key);
  }

  @BeforeEach
  void configureIngestionUrlIfMeshActive() throws Exception {
    if (MeshTestSupport.isMeshActive()) {
      exec("settings", "set", "streamx.ingestion.url",
          "http://localhost:" + MeshTestSupport.getProxyPort());
    }
  }

  @AfterEach
  void cleanupProcess() {
    if (process != null && process.isAlive()) {
      process.destroyForcibly();
    }
    envVars.clear();
  }

  protected ProcessResult execWithStdin(InputStream stdin, String... args) throws Exception {
    return execSubprocess(stdin, DEFAULT_TIMEOUT_SECONDS, args);
  }

  protected ProcessResult execWithStdin(String stdin, String... args) throws Exception {
    return execWithStdin(
        new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)),
        args
    );
  }

  protected ProcessResult execWithStdin(
      InputStream stdin,
      long timeoutSeconds,
      String... args
  ) throws Exception {
    return execSubprocess(stdin, timeoutSeconds, args);
  }

  protected ProcessResult exec(String... args) throws Exception {
    return execWithStdin(InputStream.nullInputStream(), args);
  }

  private Process startProcess(String... args) throws IOException {
    ArrayList<String> command = new ArrayList<>(CliArtifact.getExecutablePath());
    command.addAll(List.of(args));

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(false);
    pb.environment().put("STREAMX_HOME", streamxHome.toAbsolutePath().toString());
    pb.environment().putAll(envVars);
    process = pb.start();
    return process;
  }

  private ProcessResult execSubprocess(
      InputStream stdin,
      long timeoutSeconds,
      String... args
  ) throws Exception {
    startProcess(args);

    StreamCapture stdoutCapture = captureAndForward(process.getInputStream(), System.out);
    StreamCapture stderrCapture = captureAndForward(process.getErrorStream(), System.err);

    Thread stdinWriter = Thread.ofVirtual().start(() -> {
      try (OutputStream os = process.getOutputStream()) {
        stdin.transferTo(os);
        os.flush();
      } catch (Exception expected) {
      }
    });

    boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
    if (!finished) {
      process.destroyForcibly();
      Assertions.fail("Process timed out after %d seconds.\nSTDOUT: %s\nSTDERR: %s"
          .formatted(timeoutSeconds, stdoutCapture.join(), stderrCapture.join()));
    }

    stdinWriter.join();
    String stdout = stdoutCapture.join();
    String stderr = stderrCapture.join();

    return new ProcessResult(process.exitValue(), stdout, stderr);
  }

  private record StreamCapture(Thread thread, ByteArrayOutputStream buffer) {
    String join() throws InterruptedException {
      thread.join();
      return buffer.toString(StandardCharsets.UTF_8);
    }
  }

  private StreamCapture captureAndForward(InputStream source, PrintStream target) {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    Thread thread = Thread.ofVirtual().start(() -> {
      try {
        byte[] buf = new byte[1024];
        int len;
        while ((len = source.read(buf)) != -1) {
          buffer.write(buf, 0, len);
          target.write(buf, 0, len);
          target.flush();
        }
      } catch (Exception expected) {
      }
    });
    return new StreamCapture(thread, buffer);
  }

  public record ProcessResult(int exitCode, String stdout, String stderr) {

    public void assertSuccess() {
      Assertions.assertEquals(0, exitCode,
          "Expected exit code 0 but got %d.\nSTDOUT: %s\nSTDERR: %s"
              .formatted(exitCode, stdout, stderr));
    }

    /** A graceful stop is exit 0 (a handled signal) or 143 (the JVM default for SIGTERM). */
    public void assertGracefulStop() {
      Assertions.assertTrue(exitCode == 0 || exitCode == 143,
          "Expected a graceful stop (exit 0 or 143) but got %d.\nSTDOUT: %s\nSTDERR: %s"
              .formatted(exitCode, stdout, stderr));
    }

    public void assertExitCode(int expected) {
      Assertions.assertEquals(expected, exitCode,
          "Expected exit code %d but got %d.\nSTDOUT: %s\nSTDERR: %s"
              .formatted(expected, exitCode, stdout, stderr));
    }
  }

  public record AsyncProcessHandle(
      Process process,
      StreamCapture stdout,
      StreamCapture stderr
  ) {
    public String getStdout() {
      return stdout.buffer().toString(StandardCharsets.UTF_8);
    }

    public String getStderr() {
      return stderr.buffer().toString(StandardCharsets.UTF_8);
    }

    public void interruptAndJoin(long timeoutMillis) throws InterruptedException {
      process.destroy();
      if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
      }
    }

    /** Joins the capture threads first: the final output lines arrive after process death. */
    public ProcessResult toResult() {
      joinQuietly(stdout.thread());
      joinQuietly(stderr.thread());
      int exitCode = process.isAlive() ? -1 : process.exitValue();
      return new ProcessResult(exitCode, getStdout(), getStderr());
    }

    private static void joinQuietly(Thread thread) {
      try {
        thread.join(5000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  protected AsyncProcessHandle execAsync(String... args) {
    try {
      startProcess(args);

      StreamCapture stdoutCapture = captureAndForward(process.getInputStream(), System.out);
      StreamCapture stderrCapture = captureAndForward(process.getErrorStream(), System.err);
      return new AsyncProcessHandle(process, stdoutCapture, stderrCapture);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
