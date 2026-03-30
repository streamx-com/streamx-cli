package com.streamx.cli.framework;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.StreamxHome;
import io.quarkus.runtime.Quarkus;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.UsageMessageSpec;


/**
 * Each CLI command should extend this class.
 *
 * @param <ResultT> Must be serializable by Jackson (POJO, JsonSerializable, etc.)
 */
public abstract class AbstractCommand<ResultT> implements Runnable {
  @CommandLine.Spec
  public CommandSpec spec;

  @CommandLine.Spec
  public void setSpec(CommandSpec spec) {
    this.spec = spec;
    applyHiddenOptions();

    spec.usageMessage().sortOptions(false);
    reorderSections();
  }

  private void reorderSections() {
    List<String> keys = new java.util.ArrayList<>(
        spec.usageMessage().sectionKeys()
    );
    int optIdx = keys.indexOf(UsageMessageSpec.SECTION_KEY_OPTION_LIST);
    int cmdIdx = keys.indexOf(UsageMessageSpec.SECTION_KEY_COMMAND_LIST);
    if (optIdx >= 0 && cmdIdx >= 0 && optIdx < cmdIdx) {
      keys.remove(cmdIdx);
      keys.add(optIdx, UsageMessageSpec.SECTION_KEY_COMMAND_LIST);
      int headIdx = keys.indexOf(
          UsageMessageSpec.SECTION_KEY_COMMAND_LIST_HEADING
      );
      if (headIdx >= 0) {
        keys.remove(headIdx);
        keys.add(optIdx, UsageMessageSpec.SECTION_KEY_COMMAND_LIST_HEADING);
      }
      spec.usageMessage().sectionKeys(keys);
    }
  }

  @CommandLine.Option(
      names = {CommonOption.VERBOSE_SHORT, CommonOption.VERBOSE_LONG},
      description = "Print debug information"
  )
  public boolean verbose;

  @CommandLine.Option(
      names = {CommonOption.OUTPUT_SHORT, CommonOption.OUTPUT_LONG},
      description = "Specify output format: text, json, yaml",
      defaultValue = "text"
  )
  // Explicitly set default value here as a fallback for commands with the hidden output option.
  public OutputFormat output = OutputFormat.text;

  @CommandLine.ArgGroup(exclusive = false, heading = "%nGlobal Options:%n", order = 100)
  HelpOptions helpOptions = new HelpOptions();

  // Override this method to implement the command logic.
  public abstract CommandResult<ResultT> runCommand();

  // Override this method to hide specific command line options.
  // May be useful to hide the "--output" option for
  // commands that don't print anything in case of success.
  public List<String> getHiddenOptions() {
    return List.of();
  }

  // Override this method to provide human-readable output.
  public String getTextOutput(CommandResult<ResultT> result) {
    return result.toText(OutputFormat.json, null);
  }

  private void applyHiddenOptions() {
    List<String> options = getHiddenOptions();

    for (String option : options) {
      OptionSpec optionSpec = spec.findOption(option);
      if (optionSpec != null) {
        spec.remove(optionSpec);
      }
    }
  }

  public void printUsage() {
    spec.commandLine().usage(System.out);
  }

  // Use this method for asking user input in interactive commands.
  public String promptForInput(
      String prompt,
      @Nullable List<String> autocompleteOptions
  ) {
    try (Terminal terminal = createTerminal()) {
      LineReaderBuilder builder = LineReaderBuilder.builder()
          .terminal(terminal);

      Completer completer;
      if (autocompleteOptions != null) {
        completer = new StringsCompleter(autocompleteOptions);
        builder.completer(completer);
      }

      LineReader reader = builder.build();

      return reader.readLine(prompt).strip();
    } catch (IOException e) {
      throw new CliException(msg.failedToHandleInteractiveInput(), e);
    }
  }

  public int handleExecutionError(Exception e) {
    writeErrorToTempDir(e);

    if (verbose) {
      // Print exception stacktrace
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      System.err.println(sw);
    }

    return ShortErrorMessageHandler.shortErrorMessage(e, spec.commandLine());
  }

  private void writeErrorToTempDir(Exception e) {
    try {
      DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
      String prefix = "streamx-cli-" + LocalDate.now().format(dateFormat) + "-";
      Path tempDir = Files.createTempDirectory(prefix);
      Path errorFile = tempDir.resolve("error.log");
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      Files.writeString(errorFile, sw.toString());
      System.err.println(msg.errorDetailsSavedTo(errorFile.toAbsolutePath().toString()));
    } catch (IOException ignored) {
      // Best-effort; don't fail the CLI because of logging
    }
  }

  public int execute() {
    if (helpOptions.streamxHome != null) {
      StreamxHome.setStreamxHomeCliArg(helpOptions.streamxHome);
    }

    int exitCode = 0;

    try {
      CommandResult<ResultT> result = this.runCommand();
      String textOutput = result.toText(output, this::getTextOutput);
      if (!textOutput.isEmpty()) {
        System.out.println(textOutput);
      }

      if (result.getError().isPresent()) {
        exitCode = handleExecutionError(result.getError().get());
      }

      if (result.getExitCodeOverride().isPresent()) {
        exitCode = result.getExitCodeOverride().get();
      }
    } catch (Exception e) {
      exitCode = handleExecutionError(e);
    }

    System.out.flush();
    System.err.flush();

    return exitCode;
  }

  public void run() {
    int exitCode = execute();
    Quarkus.asyncExit(exitCode);
  }

  private Terminal createTerminal() throws IOException {
    if (System.console() != null) {
      return TerminalBuilder.builder().system(true).build();
    }

    return TerminalBuilder.builder()
        .system(false)
        .streams(System.in, System.err)
        .build();
  }
}
