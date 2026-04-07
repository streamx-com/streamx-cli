package com.streamx.cli.test;

import com.streamx.cli.commands.StreamxCommand;
import com.streamx.cli.framework.AbstractCommand;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableInstance;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

public abstract class CliBaseIT {

  private static final long DEFAULT_TIMEOUT_SECONDS = 30;

  protected static final String CONFIG_FILE_PATH =
      "config/application.properties";

  @TempDir
  public static Path streamxHome;

  private Process process;
  private final Map<String, String> envVars = new HashMap<>();

  private static boolean isNative() {
    return "true".equals(System.getProperty("native.image"));
  }

  protected static Path getConfigPath() {
    return streamxHome.resolve(CONFIG_FILE_PATH);
  }

  @BeforeAll
  static void ensureBuilt() {
    System.out.println("STREAMX_HOME path is " + streamxHome.toAbsolutePath());
    if (isNative()) {
      BuildExecutableOnce.ensureBuilt();
    }
  }

  /**
   * Sets a variable that will be passed as a system property in JVM mode
   * and as a real environment variable in native mode.
   */
  protected void setEnv(String key, String value) {
    if (isNative()) {
      envVars.put(key, value);
    } else {
      System.setProperty(key, value);
    }
  }

  /**
   * Removes a variable previously set via {@link #setEnv}.
   */
  protected void clearEnv(String key) {
    if (isNative()) {
      envVars.remove(key);
    } else {
      System.clearProperty(key);
    }
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
    if (!isNative()) {
      for (String key : envVars.keySet()) {
        System.clearProperty(key);
      }
    }
    envVars.clear();
  }

  protected ProcessResult execWithStdin(InputStream stdin, String... args) throws Exception {
    if (isNative()) {
      return execSubprocess(stdin, DEFAULT_TIMEOUT_SECONDS, args);
    }
    return execInProcess(stdin, args);
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
    if (isNative()) {
      return execSubprocess(stdin, timeoutSeconds, args);
    }
    return execInProcess(stdin, args);
  }

  protected ProcessResult exec(String... args) throws Exception {
    return execWithStdin(InputStream.nullInputStream(), args);
  }

  /** In-process execution for JVM mode. */
  private ProcessResult execInProcess(InputStream stdin, String... args) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();

    InputStream originalIn = System.in;
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;

    try {
      System.setIn(stdin);
      System.setOut(new PrintStream(out));
      System.setErr(new PrintStream(err));
      System.setProperty("STREAMX_HOME", streamxHome.toAbsolutePath().toString());

      int exitCode = createCommandLine().execute(args);

      return new ProcessResult(
          exitCode,
          out.toString(StandardCharsets.UTF_8),
          err.toString(StandardCharsets.UTF_8)
      );
    } finally {
      System.clearProperty("STREAMX_HOME");
      System.setIn(originalIn);
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
  }

  /** Creates a CommandLine instance for in-process execution. */
  protected CommandLine createCommandLine() {
    ArcContainer container = Arc.container();
    CommandLine cmd = new CommandLine(new StreamxCommand(), new CommandLine.IFactory() {
      @Override
      public <K> K create(Class<K> cls) throws Exception {
        InjectableInstance<K> instance = container.select(cls);
        if (instance.isResolvable()) {
          return instance.get();
        }
        return CommandLine.defaultFactory().create(cls);
      }
    });

    cmd.setExecutionStrategy(parseResult -> {
      Assertions.assertNotNull(parseResult);
      List<CommandLine> parsed = parseResult.asCommandLineList();
      CommandLine last = parsed.getLast();
      Object command = last.getCommand();

      if (command instanceof AbstractCommand<?> abstractCommand) {
        try {
          abstractCommand.populateStreamxHome();
        } catch (Exception e) {
          return abstractCommand.handleExecutionError(e);
        }
      }

      CommandLine.ParseResult pr = parseResult;
      while (pr != null) {
        if (pr.isUsageHelpRequested() || pr.isVersionHelpRequested()) {
          return new CommandLine.RunLast().execute(parseResult);
        }

        pr = pr.hasSubcommand() ? pr.subcommand() : null;
      }

      if (command instanceof AbstractCommand<?> abstractCommand) {
        return abstractCommand.execute();
      }
      return new CommandLine.RunLast().execute(parseResult);
    });

    return cmd;
  }

  /** Sub-process execution for native mode. */
  private ProcessResult execSubprocess(
      InputStream stdin,
      long timeoutSeconds,
      String... args
  ) throws Exception {
    ArrayList<String> command = new ArrayList<>(BuildExecutableOnce.getExecutablePath());
    command.addAll(List.of(args));

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(false);
    pb.environment().put("STREAMX_HOME", streamxHome.toAbsolutePath().toString());
    pb.environment().putAll(envVars);
    process = pb.start();

    StreamCapture stdoutCapture = captureAndForward(process.getInputStream(), System.out);
    StreamCapture stderrCapture = captureAndForward(process.getErrorStream(), System.err);

    Thread stdinWriter = Thread.ofVirtual().start(() -> {
      try (OutputStream os = process.getOutputStream()) {
        stdin.transferTo(os);
        os.flush();
      } catch (Exception ignored) {
        // ignore
      }
    });

    boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
    Assertions.assertTrue(finished,
        "Process timed out after %d seconds".formatted(timeoutSeconds));

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
      } catch (Exception ignored) {
        // ignore
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

    public void assertExitCode(int expected) {
      Assertions.assertEquals(expected, exitCode,
          "Expected exit code %d but got %d.\nSTDOUT: %s\nSTDERR: %s"
              .formatted(expected, exitCode, stdout, stderr));
    }
  }

  public record AsyncProcessHandle(
      Thread thread,
      ByteArrayOutputStream stdout,
      ByteArrayOutputStream stderr,
      AtomicInteger exitCode
  ) {
    public String getStdout() {
      return stdout.toString(StandardCharsets.UTF_8);
    }

    public String getStderr() {
      return stderr.toString(StandardCharsets.UTF_8);
    }

    public void interruptAndJoin(long timeoutMillis) throws InterruptedException {
      thread.interrupt();
      thread.join(timeoutMillis);
    }

    public ProcessResult toResult() {
      return new ProcessResult(exitCode.get(), getStdout(), getStderr());
    }
  }

  protected AsyncProcessHandle execAsync(String... args) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    AtomicInteger exitCode = new AtomicInteger(-1);

    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;

    PrintStream teeOut = new PrintStream(new TeeOutputStream(out, originalOut), true);
    PrintStream teeErr = new PrintStream(new TeeOutputStream(err, originalErr), true);

    Thread thread = Thread.ofVirtual().start(() -> {
      System.setOut(teeOut);
      System.setErr(teeErr);
      System.setProperty("STREAMX_HOME", streamxHome.toAbsolutePath().toString());
      try {
        exitCode.set(createCommandLine().execute(args));
      } finally {
        System.clearProperty("STREAMX_HOME");
        System.setOut(originalOut);
        System.setErr(originalErr);
      }
    });

    return new AsyncProcessHandle(thread, out, err, exitCode);
  }

  private static class TeeOutputStream extends OutputStream {
    private final OutputStream buffer;
    private final OutputStream console;

    TeeOutputStream(OutputStream buffer, OutputStream console) {
      this.buffer = buffer;
      this.console = console;
    }

    @Override
    public void write(int b) throws IOException {
      buffer.write(b);
      console.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      buffer.write(b, off, len);
      console.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
      buffer.flush();
      console.flush();
    }
  }
}