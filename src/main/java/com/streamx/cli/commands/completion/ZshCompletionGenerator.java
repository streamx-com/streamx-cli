package com.streamx.cli.commands.completion;

import com.streamx.cli.commands.settings.SettingsKeyCompletionCandidates;
import com.streamx.cli.commands.settings.SettingsSetKeyCompletionCandidates;
import com.streamx.cli.commands.settings.eventtemplates.NonDefaultTemplateIdCompletionCandidates;
import com.streamx.cli.commands.settings.eventtemplates.RegisteredTemplateIdCompletionCandidates;
import com.streamx.cli.commands.settings.eventtemplates.TemplateIdCompletionCandidates;
import com.streamx.cli.config.ContextNameCompletionCandidates;
import com.streamx.cli.platform.ClusterIdCompletionCandidates;
import com.streamx.cli.platform.InvitedEmailCompletionCandidates;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.OrgMemberIdCompletionCandidates;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;

public final class ZshCompletionGenerator {

  private static final String ORG_FROM_WORDS = "\"${words[${words[(i)--org]}+1]}\"";

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
      StringBuilder sb, String functionName, CommandLine commandLine) {
    CommandSpec spec = commandLine.getCommandSpec();
    Map<String, CommandLine> subcommands = visibleSubcommands(commandLine);
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
      Map<String, CommandLine> subcommands) {
    sb.append("  local curcontext=\"$curcontext\"\n\n");
    sb.append("  if (( CURRENT == 2 )); then\n");

    sb.append("    local -a cmd_names=(");
    for (Map.Entry<String, CommandLine> entry : subcommands.entrySet()) {
      sb.append(" '").append(entry.getKey()).append("'");
    }
    sb.append(" )\n");

    sb.append("    local -a cmd_descs=(");
    for (Map.Entry<String, CommandLine> entry : subcommands.entrySet()) {
      String desc = getCommandDescription(entry.getValue().getCommandSpec());
      sb.append(" '").append(entry.getKey()).append(" -- ")
          .append(escapeShellString(desc)).append("'");
    }
    sb.append(" )\n");
    sb.append("    compadd -V 1-commands -l -d cmd_descs -a cmd_names\n");

    if (!options.isEmpty()) {
      sb.append("    local -a opt_names=(");
      for (OptionSpec opt : options) {
        sb.append(" '").append(preferredOptionName(opt)).append("'");
      }
      sb.append(" )\n");
      sb.append("    local -a opt_descs=(");
      for (OptionSpec opt : options) {
        String desc = getFirstLine(opt.description());
        sb.append(" '").append(optionLabel(opt)).append(" -- ")
            .append(escapeShellString(desc)).append("'");
      }
      sb.append(" )\n");
      sb.append("    compadd -V 2-options -l -d opt_descs -a opt_names\n");
    }

    sb.append("  else\n");
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
      List<PositionalParamSpec> positionals) {
    List<PositionalParamSpec> actionablePositionals = positionals.stream()
        .filter(p -> !getCompletionAction(p).isEmpty())
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
    String action = getCompletionAction(opt.type(), opt, opt.completionCandidates());
    if (label == null || label.isEmpty()) {
      label = "value";
    }
    return ":" + escape(label) + ":" + action;
  }

  private static String formatPositional(PositionalParamSpec param) {
    String desc = getFirstLine(param.description());
    String action = getCompletionAction(param);
    boolean optional = param.arity().min() == 0;
    String spec;
    if (optional) {
      spec = "'::" + escape(desc) + ":" + action + "'";
    } else {
      spec = "':" + escape(desc) + ":" + action + "'";
    }
    return spec;
  }

  private static String getCompletionAction(PositionalParamSpec param) {
    return getCompletionAction(param.type(), null, param.completionCandidates());
  }

  private static String getCompletionAction(
      Class<?> type, OptionSpec opt, Iterable<String> completionCandidates) {
    if (completionCandidates instanceof RegisteredTemplateIdCompletionCandidates) {
      return "($(streamx __complete-registered-template-ids 2>/dev/null))";
    }
    if (completionCandidates instanceof NonDefaultTemplateIdCompletionCandidates) {
      return "($(streamx __complete-non-default-template-ids 2>/dev/null))";
    }
    if (completionCandidates instanceof TemplateIdCompletionCandidates) {
      return "($(streamx __complete-template-ids 2>/dev/null))";
    }
    if (completionCandidates instanceof SettingsSetKeyCompletionCandidates) {
      return "($(streamx __complete-settings-set-keys 2>/dev/null))";
    }
    if (completionCandidates instanceof SettingsKeyCompletionCandidates) {
      return "($(streamx __complete-settings-keys 2>/dev/null))";
    }
    if (completionCandidates instanceof ContextNameCompletionCandidates) {
      return "($(streamx __complete-context-names 2>/dev/null))";
    }
    if (completionCandidates instanceof OrgIdCompletionCandidates) {
      return "($(streamx __complete-org-ids 2>/dev/null))";
    }
    if (completionCandidates instanceof OrgMemberIdCompletionCandidates) {
      return "($(streamx __complete-org-member-ids " + ORG_FROM_WORDS + " 2>/dev/null))";
    }
    if (completionCandidates instanceof InvitedEmailCompletionCandidates) {
      return "($(streamx __complete-invited-emails " + ORG_FROM_WORDS + " 2>/dev/null))";
    }
    if (completionCandidates instanceof ClusterIdCompletionCandidates) {
      return "($(streamx __complete-cluster-ids " + ORG_FROM_WORDS + " 2>/dev/null))";
    }
    // Any remaining candidates are a fixed list (e.g. roles); the dynamic ones are handled above.
    if (completionCandidates != null) {
      String values = renderCandidates(completionCandidates);
      if (!values.isEmpty()) {
        return values;
      }
    }
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

  private static String renderCandidates(Iterable<String> completionCandidates) {
    StringBuilder values = new StringBuilder("(");
    boolean empty = true;
    for (String candidate : completionCandidates) {
      if (!empty) {
        values.append(" ");
      }
      values.append(escape(candidate));
      empty = false;
    }
    return empty ? "" : values.append(")").toString();
  }

  private static String preferredOptionName(OptionSpec opt) {
    String shortName = null;
    String longName = null;
    for (String name : opt.names()) {
      if (name.startsWith("--")) {
        longName = name;
      } else {
        shortName = name;
      }
    }
    return longName != null ? longName : shortName;
  }

  private static String optionLabel(OptionSpec opt) {
    String shortName = null;
    String longName = null;
    for (String name : opt.names()) {
      if (name.startsWith("--")) {
        longName = name;
      } else {
        shortName = name;
      }
    }
    if (shortName != null && longName != null) {
      return shortName + " " + longName;
    }
    return longName != null ? longName : shortName;
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

  private static Map<String, CommandLine> visibleSubcommands(CommandLine commandLine) {
    java.util.LinkedHashMap<String, CommandLine> result = new java.util.LinkedHashMap<>();
    for (Map.Entry<String, CommandLine> entry : commandLine.getSubcommands().entrySet()) {
      if (entry.getValue().getCommandSpec().usageMessage().hidden()) {
        continue;
      }
      result.put(entry.getKey(), entry.getValue());
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

  private static String escapeShellString(String text) {
    if (text == null) {
      return "";
    }
    return text.replace("'", "'\\''");
  }

  private static String sanitize(String name) {
    return name.replace("-", "_");
  }
}
