package com.streamx.cli.commands.completion;

import com.streamx.cli.commands.settings.SettingsKeyCompletionCandidates;
import com.streamx.cli.commands.settings.SettingsSetKeyCompletionCandidates;
import com.streamx.cli.commands.settings.eventtemplates.NonDefaultTemplateIdCompletionCandidates;
import com.streamx.cli.commands.settings.eventtemplates.RegisteredTemplateIdCompletionCandidates;
import com.streamx.cli.commands.settings.eventtemplates.TemplateIdCompletionCandidates;
import java.util.HashMap;
import java.util.Map;
import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.PositionalParamSpec;

/**
 * Wraps picocli's built-in bash completion generator and rewrites the
 * {@code local positionals=""} lines so that completion for our positional
 * parameters resolves dynamically at completion time by shelling out to one
 * of the {@code __complete-*} hidden subcommands. This mirrors what
 * {@link ZshCompletionGenerator} does for zsh.
 */
public final class BashCompletionGenerator {

  private static final String EMPTY_POSITIONALS = "local positionals=\"\"";

  private BashCompletionGenerator() {
  }

  public static String generate(String programName, CommandLine commandLine) {
    String picocliOutput = AutoComplete.bash(programName, commandLine);
    Map<String, String> functionToCommand = new HashMap<>();
    collect("_picocli_" + sanitize(programName), commandLine, functionToCommand);
    return rewritePositionals(picocliOutput, functionToCommand, programName);
  }

  private static void collect(
      String functionPrefix, CommandLine commandLine, Map<String, String> sink) {
    CommandSpec spec = commandLine.getCommandSpec();
    for (PositionalParamSpec param : spec.positionalParameters()) {
      String completionCmd = mapCandidatesToCommand(param.completionCandidates());
      if (completionCmd != null) {
        sink.put(functionPrefix, completionCmd);
        break;
      }
    }
    for (Map.Entry<String, CommandLine> entry : commandLine.getSubcommands().entrySet()) {
      if (entry.getValue().getCommandSpec().usageMessage().hidden()) {
        continue;
      }
      String childFunction = functionPrefix + "_" + sanitize(entry.getKey());
      collect(childFunction, entry.getValue(), sink);
    }
  }

  private static String mapCandidatesToCommand(Iterable<String> candidates) {
    if (candidates instanceof RegisteredTemplateIdCompletionCandidates) {
      return "__complete-registered-template-ids";
    }
    if (candidates instanceof NonDefaultTemplateIdCompletionCandidates) {
      return "__complete-non-default-template-ids";
    }
    if (candidates instanceof TemplateIdCompletionCandidates) {
      return "__complete-template-ids";
    }
    if (candidates instanceof SettingsSetKeyCompletionCandidates) {
      return "__complete-settings-set-keys";
    }
    if (candidates instanceof SettingsKeyCompletionCandidates) {
      return "__complete-settings-keys";
    }
    return null;
  }

  private static String rewritePositionals(
      String script, Map<String, String> functionToCommand, String programName) {
    if (functionToCommand.isEmpty()) {
      return script;
    }
    String[] lines = script.split("\n", -1);
    String currentFunction = null;
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      String openedFunction = parseFunctionDeclaration(line);
      if (openedFunction != null) {
        currentFunction = openedFunction;
        continue;
      }
      if (currentFunction == null) {
        continue;
      }
      String completionCmd = functionToCommand.get(currentFunction);
      if (completionCmd == null) {
        continue;
      }
      int idx = line.indexOf(EMPTY_POSITIONALS);
      if (idx < 0) {
        continue;
      }
      String dynamic = "local positionals=\"$(" + programName + " "
          + completionCmd + " 2>/dev/null)\"";
      lines[i] = line.substring(0, idx) + dynamic
          + line.substring(idx + EMPTY_POSITIONALS.length());
    }
    return String.join("\n", lines);
  }

  private static String parseFunctionDeclaration(String line) {
    String trimmed = line.trim();
    if (!trimmed.startsWith("function _picocli_")) {
      return null;
    }
    int parenIdx = trimmed.indexOf('(');
    if (parenIdx < 0) {
      return null;
    }
    return trimmed.substring("function ".length(), parenIdx);
  }

  private static String sanitize(String name) {
    return name.replace("-", "");
  }
}
