package com.streamx.cli.framework;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.StreamxHome;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.Model.UsageMessageSpec;

public final class SynopsisHelper {

  private SynopsisHelper() {
  }

  public static void applyCustomSynopses(CommandLine commandLine) {
    applyRecursively(commandLine);
  }

  /**
   * Root help layout: drop the {@code Usage:} synopsis, show the active profile (name in bold)
   * directly under the header, then a blank line before the command list.
   */
  public static void applyRootUsageLayout(CommandLine commandLine) {
    UsageMessageSpec usage = commandLine.getCommandSpec().usageMessage();

    List<String> keys = new ArrayList<>(usage.sectionKeys());
    keys.remove(UsageMessageSpec.SECTION_KEY_SYNOPSIS_HEADING);
    keys.remove(UsageMessageSpec.SECTION_KEY_SYNOPSIS);
    usage.sectionKeys(keys);

    usage.description(msg.currentProfileHeader("@|bold " + currentProfile() + "|@"), "");
  }

  private static String currentProfile() {
    try {
      return StreamxHome.getActiveProfile();
    } catch (RuntimeException corruptOrUnreadable) {
      return StreamxHome.DEFAULT_PROFILE;
    }
  }

  private static void applyRecursively(CommandLine commandLine) {

    if (commandLine.getCommand() instanceof AbstractCommand<?>) {
      boolean isGroup = !commandLine.getSubcommands().isEmpty();
      applyCustomSynopsis(commandLine.getCommandSpec(), isGroup);
    }

    for (Map.Entry<String, CommandLine> entry
        : commandLine.getSubcommands().entrySet()) {
      applyRecursively(entry.getValue());
    }
  }

  private static void applyCustomSynopsis(CommandSpec spec, boolean isGroup) {
    StringBuilder synopsis = new StringBuilder();
    synopsis.append(getQualifiedCommandName(spec))
        .append(" ").append(msg.synopsisOptions());

    if (isGroup) {
      synopsis.append(" ").append(msg.synopsisCommand());
    } else {
      for (PositionalParamSpec param : spec.positionalParameters()) {
        String label = param.paramLabel();
        if (param.arity().min() == 0) {
          synopsis.append(" [").append(label).append("]");
        } else {
          synopsis.append(" ").append(label);
        }
      }
    }

    spec.usageMessage().customSynopsis(synopsis.toString());
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
}
