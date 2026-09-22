package com.streamx.cli.framework;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.Contexts;
import com.streamx.cli.platform.PlatformContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import picocli.CommandLine;
import picocli.CommandLine.Help;
import picocli.CommandLine.Help.Ansi.IStyle;
import picocli.CommandLine.Help.Ansi.Text;
import picocli.CommandLine.Model.ArgGroupSpec;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.Model.UsageMessageSpec;

/** Custom CLI's usage help formatting */
public final class StreamxHelp extends Help {

  private static final char REQUIRED_MARKER = '*';

  public StreamxHelp(CommandSpec spec, ColorScheme colorScheme) {
    super(spec, colorScheme);
  }


  public static void applyCustomSynopses(CommandLine commandLine) {
    applyRecursively(commandLine);
  }

  public static void applyRootUsageLayout(CommandLine commandLine) {
    UsageMessageSpec usage = commandLine.getCommandSpec().usageMessage();

    List<String> keys = new ArrayList<>(usage.sectionKeys());
    keys.remove(UsageMessageSpec.SECTION_KEY_SYNOPSIS_HEADING);
    keys.remove(UsageMessageSpec.SECTION_KEY_SYNOPSIS);
    usage.sectionKeys(keys);

    usage.description(
        msg.currentContextHeader("@|bold " + currentContext() + "|@"),
        msg.currentOrgHeader(boldOrDash(quiet(PlatformContext::effectiveOrg))),
        msg.currentProjectHeader(boldOrDash(quiet(PlatformContext::effectiveProject))),
        "");
  }

  private static String boldOrDash(String value) {
    return value == null ? "-" : "@|bold " + value + "|@";
  }

  private static String quiet(Supplier<String> supplier) {
    try {
      return supplier.get();
    } catch (RuntimeException corruptOrUnreadable) {
      return null;
    }
  }

  private static String currentContext() {
    try {
      return Contexts.getActiveContext();
    } catch (RuntimeException corruptOrUnreadable) {
      return Contexts.DEFAULT_CONTEXT;
    }
  }

  private static void applyRecursively(CommandLine commandLine) {
    Map<String, CommandLine> subcommands = commandLine.getSubcommands();

    if (commandLine.getCommand() instanceof AbstractCommand<?>) {
      applyCustomSynopsis(commandLine.getCommandSpec(), !subcommands.isEmpty());
    }

    for (CommandLine subcommand : subcommands.values()) {
      applyRecursively(subcommand);
    }
  }

  private static void applyCustomSynopsis(CommandSpec spec, boolean isGroup) {
    spec.usageMessage().customSynopsis(
        synopsis(spec, getQualifiedCommandName(spec), msg.synopsisOptions(),
            isGroup ? msg.synopsisCommand() : null));

    // picocli marks the required entries of the lists and prints the declared defaults.
    spec.usageMessage().requiredOptionMarker(REQUIRED_MARKER);
    spec.usageMessage().showDefaultValues(true);
  }

  public static String synopsis(CommandSpec spec, String commandName, String optionsPlaceholder,
      String subcommandPlaceholder) {
    StringBuilder synopsis = new StringBuilder(commandName);
    List<OptionSpec> options = spec.options().stream().filter(option -> !option.hidden()).toList();
    for (OptionSpec option : options) {
      if (isRequired(option)) {
        synopsis.append(" ").append(requiredOptionSynopsis(option));
      }
    }
    for (ArgGroupSpec group : spec.argGroups()) {
      String members = optionalGroupSynopsis(group);
      if (!members.isEmpty()) {
        synopsis.append(" [").append(members).append("]");
      }
    }
    if (options.stream().anyMatch(option -> !isRequired(option))) {
      synopsis.append(" ").append(optionsPlaceholder);
    }
    if (subcommandPlaceholder != null) {
      synopsis.append(" ").append(subcommandPlaceholder);
    } else {
      for (PositionalParamSpec param : spec.positionalParameters()) {
        if (!param.hidden()) {
          synopsis.append(" ").append(positionalSynopsis(param));
        }
      }
    }
    return synopsis.toString();
  }

  public static String defaultValue(OptionSpec option) {
    return option.typeInfo().isBoolean() ? null : option.defaultValue();
  }

  public static boolean isRequired(OptionSpec option) {
    if (!option.required()) {
      return false;
    }
    for (ArgGroupSpec group = option.group(); group != null; group = group.parentGroup()) {
      if (group.multiplicity().min() == 0) {
        return false;
      }
    }
    return true;
  }

  private static String requiredOptionSynopsis(OptionSpec option) {
    String name = option.longestName();
    return option.typeInfo().isBoolean() ? name : name + "=" + valueLabel(option);
  }

  public static String valueLabel(ArgSpec arg) {
    String label = arg.paramLabel();
    if (!arg.splitRegex().isEmpty()) {
      return label + "[" + arg.splitRegex() + "...]";
    }
    return arg.arity().max() > 1 ? label + "..." : label;
  }

  private static String optionalGroupSynopsis(ArgGroupSpec group) {
    if (group.multiplicity().min() > 0) {
      return "";
    }
    List<String> members = new ArrayList<>();
    for (OptionSpec option : group.options()) {
      if (option.required() && !option.hidden()) {
        members.add(requiredOptionSynopsis(option));
      }
    }
    return String.join(" ", members);
  }

  private static String positionalSynopsis(PositionalParamSpec param) {
    String label = valueLabel(param);
    return param.arity().min() == 0 ? "[" + label + "]" : label;
  }

  private static String getQualifiedCommandName(CommandSpec spec) {
    List<String> names = new ArrayList<>();
    CommandSpec current = spec;
    while (current != null) {
      names.addFirst(current.name());
      current = current.parent();
    }
    if (!names.isEmpty() && names.getFirst().startsWith("<")) {
      names.set(0, msg.rootCommandName());
    }
    return String.join(" ", names);
  }

  private IParamLabelRenderer labelRenderer;

  /** Display required options first in each picocli group */
  @Override
  public String optionList() {
    Comparator<OptionSpec> byName = createDefaultOptionSort();
    Comparator<OptionSpec> requiredFirst =
        Comparator.comparing((OptionSpec option) -> !isRequired(option));
    return optionList(createDefaultLayout(),
        byName == null ? requiredFirst : requiredFirst.thenComparing(byName),
        parameterLabelRenderer());
  }

  @Override
  public IParamLabelRenderer parameterLabelRenderer() {
    if (labelRenderer != null) {
      return labelRenderer;
    }
    IParamLabelRenderer base = super.parameterLabelRenderer();
    labelRenderer = new IParamLabelRenderer() {
      @Override
      public Text renderParameterLabel(ArgSpec arg, Ansi ansi, List<IStyle> styles) {
        if (arg.splitRegex().isEmpty()) {
          return base.renderParameterLabel(arg, ansi, styles);
        }
        String prefix = arg.isOption() ? base.separator() : "";
        return ansi.new Text(prefix).concat(ansi.apply(valueLabel(arg), styles));
      }

      @Override
      public String separator() {
        return base.separator();
      }
    };
    return labelRenderer;
  }

}
