package com.streamx.cli.framework;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.util.VersionProvider;
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
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.UsageMessageSpec;

@CommandLine.Command(versionProvider = VersionProvider.class)
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
      names = {CommonOptions.VERBOSE_SHORT, CommonOptions.VERBOSE_LONG},
      description = "Print debug information"
  )
  public boolean verbose;

  @CommandLine.Option(
      names = {CommonOptions.OUTPUT_SHORT, CommonOptions.OUTPUT_LONG},
      description = "Specify output format: text, json, yaml",
      defaultValue = "text"
  )

  public OutputFormat output = OutputFormat.text;

  @CommandLine.ArgGroup(exclusive = false, heading = "%nGlobal Options:%n", order = 100)
  CommonOptions helpOptions = new CommonOptions();

  public abstract CommandResult<ResultT> runCommand();

  public List<String> getHiddenOptions() {
    return List.of();
  }

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

  public String promptForInput(
      String prompt,
      @Nullable List<String> autocompleteOptions
  ) {
    return InteractivePicker.pick(prompt, autocompleteOptions);
  }

  public int handleExecutionError(Exception e) {

    if (!(e instanceof CliException)) {
      writeErrorToTempDir(e);
    }

    if (verbose) {

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
    } catch (IOException expected) {
    }
  }

  /**
   * Whether this command operates on the active context's state (settings, credentials,
   * event templates). Context-management commands return false so they keep working when the
   * selected context does not exist and the user needs to repair the selection.
   */
  public boolean needsContext() {
    return true;
  }

  public void populateStreamxHome(List<CommandLine> parsedChain) {
    // Reset first: these per-invocation statics would otherwise leak between in-JVM executions.
    StreamxHome.clearStreamxHomeCliArg();
    StreamxHome.clearContextCliArg();
    // -H/--context may sit at any level of the invocation (streamx --context x sub cmd);
    // collect across the chain, last occurrence wins.
    for (CommandLine commandLine : parsedChain) {
      if (commandLine.getCommand() instanceof AbstractCommand<?> command) {
        if (command.helpOptions.streamxHome != null) {
          StreamxHome.setStreamxHomeCliArg(command.helpOptions.streamxHome);
        }
        if (command.helpOptions.context != null) {
          StreamxHome.setContextCliArg(command.helpOptions.context);
        }
      }
    }

    StreamxHome.populate(needsContext());
  }

  public int execute() {
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
}
