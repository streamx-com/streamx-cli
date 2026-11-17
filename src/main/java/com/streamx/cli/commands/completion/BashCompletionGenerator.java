package com.streamx.cli.commands.completion;

import com.streamx.cli.commands.settings.SettingsKeyCompletionCandidates;
import com.streamx.cli.commands.settings.SettingsSetKeyCompletionCandidates;
import com.streamx.cli.commands.settings.eventtemplates.NonDefaultTemplateIdCompletionCandidates;
import com.streamx.cli.commands.settings.eventtemplates.RegisteredTemplateIdCompletionCandidates;
import com.streamx.cli.commands.settings.eventtemplates.TemplateIdCompletionCandidates;
import com.streamx.cli.platform.DomainCompletionCandidates;
import java.util.HashMap;
import java.util.Map;
import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.PositionalParamSpec;

/**
 * Adds dynamic positional and organization/project option candidates to picocli's bash script.
 * Candidates are resolved at completion time by shelling out to one
 * of the {@code __complete-*} hidden subcommands. This mirrors what
 * {@link ZshCompletionGenerator} does for zsh.
 */
public final class BashCompletionGenerator {

  private BashCompletionGenerator() {
  }

  public static String generate(String programName, CommandLine commandLine) {
    String picocliOutput = AutoComplete.bash(programName, commandLine)
        .replace("    if [[ \"${optionsWithArgs}\" =~ ${previousWord} ]]; then", """
                if [[ "${previousWord}" == *=* \\
                    && "${optionsWithArgs}" == *"'${previousWord%%=*}'"* ]]; then
                  ((result-=1))
                elif [[ "${optionsWithArgs}" =~ ${previousWord} ]]; then
              """.stripTrailing())
        .replace("((result-=2)) # Arg option and its value not counted as positional param",
            """
            if [[ "${COMP_WORDS[i+1]}" == "=" ]]; then
                    ((result-=3))
                  else
                    ((result-=2))
                  fi
            """.stripTrailing());
    Map<String, String> functionToCommand = new HashMap<>();
    collect("_picocli_" + sanitize(programName), commandLine, functionToCommand);
    return CompletionScope.functions("${COMP_WORDS[@]}", programName)
        + rewriteCompletions(picocliOutput, functionToCommand);
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
    if (candidates instanceof DomainCompletionCandidates) {
      return "__complete-domains " + CompletionScope.value("--org") + " "
          + CompletionScope.value("--project");
    }
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

  private static String rewriteCompletions(
      String script, Map<String, String> functionToCommand) {
    String[] lines = script.split("\n", -1);
    String currentFunction = null;
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      if (line.contains("COMPREPLY=") && line.contains("${orgId_option_args[@]}")) {
        lines[i] = "      COMPREPLY=( $(compgen -W \"$(_streamx_complete_scoped "
            + "__complete-org-ids 2>/dev/null)\" -- \"${curr_word}\") )";
      } else if (line.contains("COMPREPLY=") && line.contains("${projectId_option_args[@]}")) {
        lines[i] = "      COMPREPLY=( $(compgen -W \"$(_streamx_complete_scoped "
            + "__complete-project-ids " + CompletionScope.value("--org")
            + " 2>/dev/null)\" -- \"${curr_word}\") )";
      }
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
      if (line.trim().startsWith("positionals=$( compReplyArray ")) {
        lines[i] = "      positionals=\"$(_streamx_complete_scoped "
            + completionCmd + " 2>/dev/null)\"";
      }
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
