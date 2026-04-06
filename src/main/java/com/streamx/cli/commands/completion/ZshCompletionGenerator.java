package com.streamx.cli.commands.completion;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;

/**
 * Generates native zsh completion scripts from picocli's CommandSpec tree.
 * Uses zsh's {@code _arguments} and {@code _describe} for rich completions with descriptions.
 */
public class ZshCompletionGenerator {

  private ZshCompletionGenerator() {
  }

  public static String generate(String programName, CommandLine commandLine) {
    StringBuilder sb = new StringBuilder();
    sb.append("#compdef ").append(programName).append("\n\n");
    generateFunction(sb, "_" + sanitize(programName), commandLine);
    sb.append("compdef _").append(sanitize(programName))
        .append(" ").append(programName).append("\n");
    return sb.toString();
  }

  private static void generateFunction(
      StringBuilder sb, String functionName, CommandLine commandLine
  ) {
    CommandSpec spec = commandLine.getCommandSpec();
    Map<String, CommandLine> subcommands = commandLine.getSubcommands();
    List<OptionSpec> options = collectVisibleOptions(spec);
    List<PositionalParamSpec> positionals = spec.positionalParameters();

    sb.append(functionName).append("() {\n");

    if (!subcommands.isEmpty()) {
      generateSubcommandFunction(sb, functionName, options, subcommands);
    } else {
      generateLeafFunction(sb, options, positionals);
    }

    sb.append("}\n\n");

    for (Map.Entry<String, CommandLine> entry : subcommands.entrySet()) {
      String childFunctionName = functionName + "_" + sanitize(entry.getKey());
      generateFunction(sb, childFunctionName, entry.getValue());
    }
  }

  private static void generateSubcommandFunction(
      StringBuilder sb,
      String functionName,
      List<OptionSpec> options,
      Map<String, CommandLine> subcommands
  ) {
    sb.append("  local curcontext=\"$curcontext\"\n\n");

    sb.append("  if (( CURRENT == 2 )); then\n");
    sb.append("    local -a commands=(\n");

    for (Map.Entry<String, CommandLine> entry : subcommands.entrySet()) {
      String name = entry.getKey();
      String desc = getCommandDescription(entry.getValue().getCommandSpec());
      sb.append("      '").append(escape(name)).append(":")
          .append(escape(desc)).append("'\n");
    }

    sb.append("    )\n");

    if (!options.isEmpty()) {
      sb.append("    local -a opts=(\n");
      for (OptionSpec opt : options) {
        String desc = getFirstLine(opt.description());
        for (String name : opt.names()) {
          sb.append("      '").append(escape(name)).append(":")
              .append(escape(desc)).append("'\n");
        }
      }
      sb.append("    )\n");
    }

    sb.append("    _describe 'command' commands\n");
    if (!options.isEmpty()) {
      sb.append("    _describe 'option' opts\n");
    }

    sb.append("  else\n");
    // Subcommand already selected — shift words and dispatch.
    sb.append("    local subcmd=${words[2]}\n");
    sb.append("    curcontext=\"${curcontext%:*:*}:")
        .append(sanitize(functionName.substring(1)))
        .append("-${subcmd}:\"\n");
    sb.append("    (( CURRENT-- ))\n");
    sb.append("    shift words\n");
    sb.append("    case $subcmd in\n");

    for (String name : subcommands.keySet()) {
      sb.append("      ").append(name).append(") ")
          .append(functionName).append("_").append(sanitize(name)).append(" ;;\n");
    }

    sb.append("    esac\n");
    sb.append("  fi\n");
  }

  private static void generateLeafFunction(
      StringBuilder sb,
      List<OptionSpec> options,
      List<PositionalParamSpec> positionals
  ) {
    // Only include positionals that have a real completion action (files, enums).
    // Positionals with empty actions block option completion on bare TAB.
    List<PositionalParamSpec> actionablePositionals = positionals.stream()
        .filter(p -> !getCompletionAction(p.type(), null).isEmpty())
        .toList();

    if (options.isEmpty() && actionablePositionals.isEmpty()) {
      return;
    }

    sb.append("  _arguments \\\n");

    for (OptionSpec opt : options) {
      sb.append("    ").append(formatOption(opt)).append(" \\\n");
    }

    for (int i = 0; i < actionablePositionals.size(); i++) {
      PositionalParamSpec param = actionablePositionals.get(i);
      String trailing = (i < actionablePositionals.size() - 1) ? " \\" : "";
      sb.append("    ").append(formatPositional(param)).append(trailing).append("\n");
    }

    if (actionablePositionals.isEmpty()) {
      // Remove trailing backslash from last option line
      int lastBackslash = sb.lastIndexOf(" \\");
      if (lastBackslash >= 0) {
        sb.delete(lastBackslash, lastBackslash + 2);
      }
    }
  }

  private static String formatOption(OptionSpec opt) {
    String[] names = opt.names();
    String desc = getFirstLine(opt.description());
    String argSpec = getOptionArgSpec(opt);

    String shortName = null;
    String longName = null;
    for (String name : names) {
      if (name.startsWith("--")) {
        longName = name;
      } else if (name.startsWith("-")) {
        shortName = name;
      }
    }

    if (shortName != null && longName != null) {
      String exclusion = "'(" + shortName + " " + longName + ")'";
      return exclusion + "{" + shortName + "," + longName + "}"
          + "'[" + escape(desc) + "]" + argSpec + "'";
    } else {
      String name = longName != null ? longName : shortName;
      return "'" + name + "[" + escape(desc) + "]" + argSpec + "'";
    }
  }

  private static String getOptionArgSpec(OptionSpec opt) {
    if (isBoolean(opt)) {
      return "";
    }

    String label = opt.paramLabel();
    String action = getCompletionAction(opt.type(), opt);

    if (label == null || label.isEmpty()) {
      label = "value";
    }

    return ":" + escape(label) + ":" + action;
  }

  private static String formatPositional(PositionalParamSpec param) {
    String desc = getFirstLine(param.description());
    String action = getCompletionAction(param.type(), null);
    boolean optional = param.arity().min() == 0;

    String spec;
    if (optional) {
      spec = "'::" + escape(desc) + ":" + action + "'";
    } else {
      spec = "':" + escape(desc) + ":" + action + "'";
    }
    return spec;
  }

  private static String getCompletionAction(Class<?> type, OptionSpec opt) {
    if (type != null && type.isEnum()) {
      Object[] constants = type.getEnumConstants();
      StringBuilder values = new StringBuilder("(");
      for (int i = 0; i < constants.length; i++) {
        if (i > 0) {
          values.append(" ");
        }
        values.append(constants[i].toString());
      }
      values.append(")");
      return values.toString();
    }

    if (type != null && (Path.class.isAssignableFrom(type) || File.class.isAssignableFrom(type))) {
      return "_files";
    }

    return "";
  }

  private static boolean isBoolean(OptionSpec opt) {
    Class<?> type = opt.type();
    return type == boolean.class || type == Boolean.class;
  }

  private static List<OptionSpec> collectVisibleOptions(CommandSpec spec) {
    List<OptionSpec> result = new ArrayList<>();
    for (OptionSpec opt : spec.options()) {
      if (opt.hidden()) {
        continue;
      }
      result.add(opt);
    }
    return result;
  }

  private static String getCommandDescription(CommandSpec spec) {
    String[] header = spec.usageMessage().header();
    if (header != null && header.length > 0 && !header[0].isEmpty()) {
      return header[0];
    }
    String[] desc = spec.usageMessage().description();
    if (desc != null && desc.length > 0 && !desc[0].isEmpty()) {
      return desc[0];
    }
    return spec.name();
  }

  private static String getFirstLine(String[] lines) {
    if (lines == null || lines.length == 0) {
      return "";
    }
    return lines[0];
  }

  private static String escape(String text) {
    if (text == null) {
      return "";
    }
    return text
        .replace("'", "'\\''")
        .replace("[", "\\[")
        .replace("]", "\\]")
        .replace(":", "\\:");
  }

  private static String sanitize(String name) {
    return name.replace("-", "_");
  }
}
