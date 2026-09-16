package com.streamx.cli.docs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;

/**
 * Renders the picocli command tree as a Docusaurus documentation tree.
 *
 * <p>A command with subcommands becomes a folder holding {@code index.md} plus a
 * {@code _category_.json}; a leaf command becomes a single {@code .md}. Options shared by every
 * command are documented once in {@code global-options.md} and linked, so each page shows only
 * what is specific to it.
 */
public final class MarkdownDocsGenerator {

  private static final String GLOBAL_OPTIONS_PAGE = "global-options";

  /**
   * Options shared by every command that actually does something. They come from the common base
   * rather than picocli inheritance, and command groups strip some of them, so the set is derived
   * as the intersection over all leaf commands.
   */
  private static Set<String> globalOptionNames = Set.of();

  private MarkdownDocsGenerator() {
  }

  /** Writes the whole tree under {@code outputDir}, and returns how many pages were written. */
  public static int generate(CommandLine root, Path outputDir) {
    try {
      Files.createDirectories(outputDir);
      globalOptionNames = commonLeafOptionNames(root);
      writeCategory(outputDir, "Commands", 2, false);
      int pages = writeGlobalOptions(root, outputDir);
      return pages + writeCommand(root, outputDir, List.of(), 1);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot write documentation to " + outputDir, e);
    }
  }

  private static int writeCommand(CommandLine commandLine, Path dir, List<String> path,
      int position) throws IOException {
    CommandSpec spec = commandLine.getCommandSpec();
    Map<String, CommandLine> children = visibleSubcommands(commandLine);
    List<String> fullPath = new ArrayList<>(path);
    fullPath.add(spec.name());

    String body = renderPage(spec, fullPath, children);
    int written = 1;

    if (children.isEmpty()) {
      Files.writeString(dir.resolve(spec.name() + ".md"), frontMatter(spec, fullPath, position)
          + body, StandardCharsets.UTF_8);
      return written;
    }

    Path childDir = path.isEmpty() ? dir : dir.resolve(spec.name());
    Files.createDirectories(childDir);
    if (!path.isEmpty()) {
      writeCategory(childDir, spec.name(), position, true);
    }
    Files.writeString(childDir.resolve("index.md"), frontMatter(spec, fullPath, 0) + body,
        StandardCharsets.UTF_8);

    int childPosition = 1;
    for (CommandLine child : children.values()) {
      written += writeCommand(child, childDir, fullPath, childPosition++);
    }
    return written;
  }

  private static String renderPage(CommandSpec spec, List<String> fullPath,
      Map<String, CommandLine> children) {
    String command = String.join(" ", fullPath);
    StringBuilder md = new StringBuilder();

    md.append("# `").append(command).append("`\n\n");

    String header = firstLine(spec.usageMessage().header());
    if (!header.isEmpty()) {
      md.append(mdx(header)).append("\n\n");
    }

    // Plain text, not bash: the synopsis is a usage template, and several subcommand names
    // (local, set, unset) are shell builtins that a bash grammar would colour as keywords.
    md.append("```text\n").append(synopsis(spec, command, children)).append("\n```\n\n");

    // The root's description is injected at runtime by SynopsisHelper (active context, current
    // org and project), so it is machine state rather than documentation.
    if (fullPath.size() > 1) {
      md.append(describe(spec.usageMessage().description()));
    }

    appendSubcommands(md, fullPath, children);
    appendPositionals(md, spec);
    appendOptions(md, spec);

    md.append("---\n\n")
        .append("Every command also accepts the [global options](")
        .append(globalOptionsLink(fullPath, !children.isEmpty())).append(").\n");
    return md.toString();
  }

  /**
   * Help text is plain terminal output, not markdown: indentation and line breaks carry meaning.
   * Indented runs (setup snippets) are emitted as code blocks so they keep their shape, and prose
   * lines keep their breaks with a trailing backslash.
   */
  private static String describe(String[] description) {
    if (description == null || description.length == 0) {
      return "";
    }
    List<String> lines = Arrays.asList(description);
    StringBuilder out = new StringBuilder();
    int i = 0;
    while (i < lines.size()) {
      if (lines.get(i).isBlank()) {
        i++;
        continue;
      }
      boolean indented = lines.get(i).startsWith(" ") || lines.get(i).startsWith("\t");
      List<String> block = new ArrayList<>();
      while (i < lines.size() && !lines.get(i).isBlank()
          && (lines.get(i).startsWith(" ") || lines.get(i).startsWith("\t")) == indented) {
        block.add(lines.get(i));
        i++;
      }
      if (indented) {
        out.append("```bash\n");
        block.forEach(line -> out.append(line.strip()).append("\n"));
        out.append("```\n\n");
      } else {
        out.append(String.join("\\\n", block.stream().map(MarkdownDocsGenerator::mdx).toList()))
            .append("\n\n");
      }
    }
    return out.toString();
  }

  private static void appendSubcommands(StringBuilder md, List<String> fullPath,
      Map<String, CommandLine> children) {
    if (children.isEmpty()) {
      return;
    }
    md.append("## Subcommands\n\n| Command | Description |\n| --- | --- |\n");
    for (Map.Entry<String, CommandLine> entry : children.entrySet()) {
      CommandSpec child = entry.getValue().getCommandSpec();
      String link = visibleSubcommands(entry.getValue()).isEmpty()
          ? "./" + entry.getKey()
          : "./" + entry.getKey() + "/";
      md.append("| [`").append(entry.getKey()).append("`](").append(link).append(") | ")
          .append(cell(firstLine(child.usageMessage().header()))).append(" |\n");
    }
    md.append("\n");
  }

  private static void appendPositionals(StringBuilder md, CommandSpec spec) {
    List<PositionalParamSpec> positionals = spec.positionalParameters().stream()
        .filter(p -> !p.hidden())
        .toList();
    if (positionals.isEmpty()) {
      return;
    }
    md.append("## Arguments\n\n| Argument | Required | Description |\n| --- | --- | --- |\n");
    for (PositionalParamSpec positional : positionals) {
      md.append("| `").append(positional.paramLabel()).append("` | ")
          .append(positional.arity().min() > 0 ? "yes" : "no").append(" | ")
          .append(cell(join(positional.description()))).append(" |\n");
    }
    md.append("\n");
  }

  private static void appendOptions(StringBuilder md, CommandSpec spec) {
    List<OptionSpec> options = localOptions(spec).stream()
        .sorted(Comparator.comparing(MarkdownDocsGenerator::primaryName))
        .toList();
    if (options.isEmpty()) {
      return;
    }
    md.append("## Options\n\n| Option | Value | Description |\n| --- | --- | --- |\n");
    for (OptionSpec option : options) {
      md.append("| `").append(String.join("`, `", option.names())).append("` | ")
          .append(option.typeInfo().isBoolean() ? "" : "`" + option.paramLabel() + "`")
          .append(" | ").append(cell(join(option.description()))).append(" |\n");
    }
    md.append("\n");
  }

  /** Option names present on every leaf command - those are the ones that work everywhere. */
  private static Set<String> commonLeafOptionNames(CommandLine root) {
    List<Set<String>> perLeaf = new ArrayList<>();
    collectLeafOptionNames(root, perLeaf);
    if (perLeaf.isEmpty()) {
      return Set.of();
    }
    Set<String> shared = new java.util.LinkedHashSet<>(perLeaf.get(0));
    perLeaf.forEach(shared::retainAll);
    return Set.copyOf(shared);
  }

  private static void collectLeafOptionNames(CommandLine commandLine, List<Set<String>> into) {
    Map<String, CommandLine> children = visibleSubcommands(commandLine);
    if (children.isEmpty()) {
      into.add(commandLine.getCommandSpec().options().stream()
          .map(MarkdownDocsGenerator::primaryName)
          .collect(Collectors.toCollection(java.util.LinkedHashSet::new)));
      return;
    }
    children.values().forEach(child -> collectLeafOptionNames(child, into));
  }

  /** Renders the shared options once, taking their descriptions from any command that has them. */
  private static int writeGlobalOptions(CommandLine root, Path outputDir) throws IOException {
    Map<String, OptionSpec> shared = new TreeMap<>();
    collectSharedOptionSpecs(root, shared);
    CommandSpec spec = root.getCommandSpec();
    StringBuilder md = new StringBuilder();
    md.append("---\ntitle: Global options\nsidebar_label: Global options\n")
        .append("sidebar_position: 999\n---\n\n")
        .append("# Global options\n\n")
        .append("These options are accepted by every `").append(spec.name())
        .append("` command, at any position in the invocation.\n\n")
        .append("| Option | Value | Description |\n| --- | --- | --- |\n");
    shared.values().stream()
        .sorted(Comparator.comparing(MarkdownDocsGenerator::primaryName))
        .forEach(option -> md.append("| `").append(String.join("`, `", option.names()))
            .append("` | ").append(option.typeInfo().isBoolean() ? "" : "`"
                + option.paramLabel() + "`")
            .append(" | ").append(cell(join(option.description()))).append(" |\n"));
    Files.writeString(outputDir.resolve(GLOBAL_OPTIONS_PAGE + ".md"), md.toString(),
        StandardCharsets.UTF_8);
    return 1;
  }

  private static void collectSharedOptionSpecs(CommandLine commandLine,
      Map<String, OptionSpec> into) {
    commandLine.getCommandSpec().options().stream()
        .filter(option -> globalOptionNames.contains(primaryName(option)))
        .forEach(option -> into.putIfAbsent(primaryName(option), option));
    visibleSubcommands(commandLine).values()
        .forEach(child -> collectSharedOptionSpecs(child, into));
  }

  private static String synopsis(CommandSpec spec, String command,
      Map<String, CommandLine> children) {
    StringBuilder synopsis = new StringBuilder(command);
    if (!children.isEmpty()) {
      synopsis.append(" <command>");
    }
    if (!localOptions(spec).isEmpty()) {
      synopsis.append(" [options]");
    }
    for (PositionalParamSpec positional : spec.positionalParameters()) {
      if (positional.hidden()) {
        continue;
      }
      synopsis.append(positional.arity().min() > 0
          ? " " + positional.paramLabel()
          : " [" + positional.paramLabel() + "]");
    }
    return synopsis.toString();
  }

  private static String frontMatter(CommandSpec spec, List<String> fullPath, int position) {
    String header = firstLine(spec.usageMessage().header());
    return "---\ntitle: \"" + String.join(" ", fullPath) + "\"\n"
        + "sidebar_label: \"" + spec.name() + "\"\n"
        + "sidebar_position: " + position + "\n"
        + (header.isEmpty() ? "" : "description: \"" + escapeYaml(header) + "\"\n")
        + "---\n\n";
  }

  /**
   * No {@code link} here on purpose: Docusaurus then uses the folder's own {@code index.md} as the
   * category page, so the group is not listed twice (once as a category, once as a page).
   */
  private static void writeCategory(Path dir, String label, int position, boolean collapsed)
      throws IOException {
    String json = "{\n  \"label\": \"" + label + "\",\n"
        + "  \"position\": " + position + ",\n"
        + "  \"collapsed\": " + collapsed + "\n}\n";
    Files.writeString(dir.resolve("_category_.json"), json, StandardCharsets.UTF_8);
  }

  /** Options specific to this command - the ones inherited by every command are excluded. */
  private static List<OptionSpec> localOptions(CommandSpec spec) {
    return spec.options().stream()
        .filter(option -> !option.hidden())
        .filter(option -> !globalOptionNames.contains(primaryName(option)))
        .toList();
  }

  private static String primaryName(OptionSpec option) {
    return option.longestName().replaceFirst("^-+", "");
  }

  private static Map<String, CommandLine> visibleSubcommands(CommandLine commandLine) {
    Map<String, CommandLine> visible = new TreeMap<>();
    commandLine.getSubcommands().forEach((name, child) -> {
      if (!child.getCommandSpec().usageMessage().hidden()) {
        visible.put(name, child);
      }
    });
    return visible;
  }

  /**
   * File-relative link to the shared options page. A group's page lives one directory deeper
   * than a leaf's ({@code auth/index.md} vs {@code auth/login.md}), so the two differ by a level.
   * The {@code .md} suffix lets Docusaurus resolve the target as a file rather than a URL.
   */
  private static String globalOptionsLink(List<String> fullPath, boolean isGroup) {
    int up = Math.max(0, fullPath.size() - (isGroup ? 1 : 2));
    return "../".repeat(up) + GLOBAL_OPTIONS_PAGE + ".md";
  }

  private static String join(String[] lines) {
    return lines == null ? "" : String.join(" ", lines).strip();
  }

  private static String firstLine(String[] lines) {
    return lines == null || lines.length == 0 ? "" : lines[0].strip();
  }

  private static String cell(String text) {
    return mdx(text).replace("|", "\\|").replace("\n", " ");
  }

  /**
   * Docusaurus parses markdown as MDX, so bare {@code <...>} reads as a JSX tag and {@code {...}}
   * as an expression. Help text is full of both ({@code <token>}, {@code source <(...)}), so they
   * are escaped for prose. Fenced code blocks are exempt and keep the literal text.
   */
  /** Removes picocli's ANSI styling markup, e.g. {@code @|bold value|@} leaves {@code value}. */
  private static String stripAnsiMarkup(String text) {
    return text.replaceAll("@\\|[a-zA-Z,()\\d]*\\s+(.*?)\\|@", "$1");
  }

  private static String mdx(String text) {
    return stripAnsiMarkup(text)
        .replace("\\", "\\\\")
        .replace("<", "&lt;").replace(">", "&gt;")
        .replace("{", "&#123;").replace("}", "&#125;")
        .replace("~", "\\~")
        .replace("*", "\\*")
        .replace("_", "\\_")
        .replace("`", "\\`")
        .replace("[", "\\[").replace("]", "\\]");
  }

  private static String escapeYaml(String text) {
    return text.replace("\\", "\\\\").replace("\"", "\\\"");
  }

}
