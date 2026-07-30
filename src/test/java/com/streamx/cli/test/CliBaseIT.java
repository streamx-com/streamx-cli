package com.streamx.cli.test;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import org.awaitility.Awaitility;
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

  private static final long ASYNC_COMMAND_SHUTDOWN_TIMEOUT_MILLIS = 60_000;

  private static final Path BUILD_OUTPUT_DIR = Path.of("target");

  private static final List<AsyncProcessHandle> ASYNC_COMMANDS = new ArrayList<>();

  private static final AtomicInteger ASYNC_COMMAND_COUNTER = new AtomicInteger();

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

  protected void setEnv(String key, String value) {
    envVars.put(key, value);
    if (!isNative()) {
      System.setProperty(key, value);
    }
  }

  protected void clearEnv(String key) {
    envVars.remove(key);
    if (!isNative()) {
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
      } catch (Exception expected) {
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

  record StreamCapture(Thread thread, ByteArrayOutputStream buffer) {
    String join() throws InterruptedException {
      thread.join();
      return content();
    }

    String content() {
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

    public void assertExitCode(int expected) {
      Assertions.assertEquals(expected, exitCode,
          "Expected exit code %d but got %d.\nSTDOUT: %s\nSTDERR: %s"
              .formatted(expected, exitCode, stdout, stderr));
    }
  }

  public record AsyncProcessHandle(
      Process process,
      StreamCapture stdoutCapture,
      StreamCapture stderrCapture
  ) {
    public String getStdout() {
      return stdoutCapture.content();
    }

    public String getStderr() {
      return stderrCapture.content();
    }

    public boolean isAlive() {
      return process.isAlive();
    }

    public void stopAndJoin(long timeoutMillis) throws InterruptedException {
      process.destroy();
      if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
      }
      joinCaptures(timeoutMillis);
    }

    public ProcessResult toResult() throws InterruptedException {
      if (!process.isAlive()) {
        joinCaptures(ASYNC_COMMAND_SHUTDOWN_TIMEOUT_MILLIS);
      }
      return new ProcessResult(
          process.isAlive() ? -1 : process.exitValue(), getStdout(), getStderr());
    }

    private void joinCaptures(long timeoutMillis) throws InterruptedException {
      stdoutCapture.thread().join(timeoutMillis);
      stderrCapture.thread().join(timeoutMillis);
    }
  }

  protected AsyncProcessHandle execAsync(String... args) throws IOException {
    awaitAsyncCommands();

    List<String> command = cliLaunchCommand();
    command.addAll(List.of(args));

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.environment().put("STREAMX_HOME", streamxHome.toAbsolutePath().toString());
    pb.environment().putAll(envVars);
    Process process = pb.start();

    AsyncProcessHandle handle = new AsyncProcessHandle(
        process,
        captureAndForward(process.getInputStream(), System.out),
        captureAndForward(process.getErrorStream(), System.err));
    ASYNC_COMMANDS.add(handle);
    return handle;
  }

  protected void awaitAsyncCommands() {
    for (AsyncProcessHandle handle : ASYNC_COMMANDS) {
      try {
        handle.stopAndJoin(ASYNC_COMMAND_SHUTDOWN_TIMEOUT_MILLIS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    ASYNC_COMMANDS.clear();
  }

  protected void awaitStdoutContains(AsyncProcessHandle handle, String... expected) {
    awaitOutputContains(handle, AsyncProcessHandle::getStdout, expected);
  }

  protected void awaitStderrContains(AsyncProcessHandle handle, String... expected) {
    awaitOutputContains(handle, AsyncProcessHandle::getStderr, expected);
  }

  private static void awaitOutputContains(
      AsyncProcessHandle handle,
      Function<AsyncProcessHandle, String> output,
      String... expected
  ) {
    try {
      Awaitility.await()
          .atMost(Duration.ofMinutes(3))
          .pollInterval(Duration.ofSeconds(1))
          .failFast(() -> !handle.isAlive() && !containsAll(output.apply(handle), expected))
          .untilAsserted(() -> assertThat(output.apply(handle)).contains(expected));
    } catch (RuntimeException e) {
      throw new AssertionError(
          ("Expected the CLI output to contain %s. CLI process alive: %s"
              + "%n--- captured stdout ---%n%s%n--- captured stderr ---%n%s")
              .formatted(List.of(expected), handle.isAlive(),
                  handle.getStdout(), handle.getStderr()),
          e);
    }
  }

  private static boolean containsAll(String output, String... expected) {
    for (String part : expected) {
      if (!output.contains(part)) {
        return false;
      }
    }
    return true;
  }

  private static List<String> cliLaunchCommand() {
    List<String> command = new ArrayList<>();
    if (isNative()) {
      command.addAll(BuildExecutableOnce.getExecutablePath());
      command.addAll(forwardedSystemProperties());
      return command;
    }
    command.add(ProcessHandle.current().info().command().orElseThrow());
    command.addAll(tracingAgentArguments());
    command.addAll(forwardedSystemProperties());
    command.add("-jar");
    command.add(packagedCliJar().toString());
    return command;
  }

  private static Path packagedCliJar() {
    try (Stream<Path> files = Files.list(BUILD_OUTPUT_DIR)) {
      return files
          .filter(path -> path.getFileName().toString().endsWith("-runner.jar"))
          .max(Comparator.comparing(CliBaseIT::lastModified))
          .orElseThrow(() -> new IllegalStateException(
              "Packaged CLI (*-runner.jar) not found in target; run mvn package first"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static FileTime lastModified(Path path) {
    try {
      return Files.getLastModifiedTime(path);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static List<String> tracingAgentArguments() {
    return ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
        .filter(argument -> argument.startsWith("-agentlib:native-image-agent"))
        .map(argument -> argument.replace("config-merge-dir=", "config-output-dir=")
            + "-async-" + ASYNC_COMMAND_COUNTER.incrementAndGet())
        .toList();
  }

  private static List<String> forwardedSystemProperties() {
    List<String> arguments = new ArrayList<>();
    for (String name : System.getProperties().stringPropertyNames()) {
      if (name.startsWith("streamx.") || name.startsWith("test.")) {
        arguments.add("-D" + name + "=" + System.getProperty(name));
      }
    }
    return arguments;
  }
}
