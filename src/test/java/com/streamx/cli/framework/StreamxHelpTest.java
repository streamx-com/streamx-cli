package com.streamx.cli.framework;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.docs.MarkdownDocsGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class StreamxHelpTest {

  @CommandLine.Command(name = "streamx",
      subcommands = {InviteCommand.class, ListCommand.class, CreateCommand.class})
  static class RootCommand extends AbstractCommandGroup {
  }

  @CommandLine.Command(name = "create")
  static class CreateCommand extends AbstractCommand<Void> {

    @CommandLine.ArgGroup(exclusive = false)
    RepositoryOptions repository;

    static class RepositoryOptions {

      @CommandLine.Option(names = "--repository-uri", required = true, paramLabel = "<uri>",
          description = "Repository")
      String uri;

      @CommandLine.Option(names = "--repository-branch", required = true, paramLabel = "<branch>",
          description = "Branch")
      String branch;

      @CommandLine.Option(names = "--ssh-private-key", paramLabel = "<file>", description = "Key")
      String key;
    }

    @CommandLine.Parameters(index = "0", paramLabel = "<name>", description = "Name")
    String name;

    @CommandLine.Parameters(index = "1..*", arity = "0..*", paramLabel = "<clusterId>",
        description = "Clusters")
    List<String> clusters;

    @CommandLine.Option(names = "--tags", split = ",", paramLabel = "<tag>", description = "Tags")
    List<String> tags;

    @Override
    public CommandResult<Void> runCommand() {
      return new CommandResult<>(null);
    }
  }

  @CommandLine.Command(name = "list")
  static class ListCommand extends AbstractCommand<Void> {

    @Override
    public CommandResult<Void> runCommand() {
      return new CommandResult<>(null);
    }
  }

  @CommandLine.Command(name = "invite")
  static class InviteCommand extends AbstractCommand<Void> {

    @CommandLine.Option(names = {"-r", "--role"}, required = true, paramLabel = "<role>",
        description = "Role to grant")
    String role;

    @CommandLine.Option(names = "--org", paramLabel = "<orgId>", description = "Organization")
    String org;

    @CommandLine.Option(names = "--file", paramLabel = "<path>", description = "Mesh file",
        defaultValue = "mesh.yaml")
    String file;

    @CommandLine.Parameters(index = "0", paramLabel = "<email>", description = "Email")
    String email;

    @Override
    public CommandResult<Void> runCommand() {
      return new CommandResult<>(null);
    }
  }

  @Test
  void requiredOptionsAreSpelledOutInTheHelpSynopsis() {
    CommandLine commandLine = new CommandLine(new RootCommand());

    StreamxHelp.applyCustomSynopses(commandLine);

    assertThat(commandLine.getSubcommands().get("invite").getCommandSpec().usageMessage()
        .customSynopsis())
        .containsExactly("streamx invite --role=<role> [...options] <email>");
    assertThat(commandLine.getSubcommands().get("create").getCommandSpec().usageMessage()
        .customSynopsis())
        .as("members of an optional group are not required; a list argument is marked")
        .containsExactly("streamx create [--repository-uri=<uri> --repository-branch=<branch>]"
            + " [...options] <name> [<clusterId>...]");
  }

  @Test
  void requiredOptionsAreSpelledOutInTheDocs(@TempDir Path outputDir) throws IOException {
    CommandLine commandLine = new CommandLine(new RootCommand());

    MarkdownDocsGenerator.generate(commandLine, outputDir);

    assertThat(Files.readString(outputDir.resolve("invite.md")))
        .contains("streamx invite --role=<role> [options] <email>")
        .contains("| Option | Required | Description |\n"
            + "| --- | --- | --- |\n"
            + "| `-r, --role=<role>` | yes | Role to grant |\n"
            + "| `--file=<path>` | no | Mesh file (default `mesh.yaml`) |\n"
            + "| `--org=<orgId>` | no | Organization |\n");

    assertThat(Files.readString(outputDir.resolve("create.md")))
        .contains("streamx create [--repository-uri=<uri> --repository-branch=<branch>] [options]"
            + " <name> [<clusterId>...]")
        .contains("| `--repository-branch=<branch>` | no | Branch |")
        .contains("| `<clusterId>...` | no | Clusters |")
        .contains("| `--tags=<tag>[,...]` | no | Tags |");
  }

  @Test
  void requiredOptionsAndArgumentsAreMarkedInTheLists() {
    CommandLine commandLine = new CommandLine(new RootCommand())
        .setHelpFactory(StreamxHelp::new);
    StreamxHelp.applyCustomSynopses(commandLine);

    String invite = commandLine.getSubcommands().get("invite").getUsageMessage();
    String create = commandLine.getSubcommands().get("create").getUsageMessage();

    assertThat(invite)
        .contains("*     <email>")
        .contains("* -r, --role=<role>")
        .contains("      --org=<orgId>")
        .contains("Default: mesh.yaml");
    assertThat(create)
        .as("members of an optional group are not required at the command level")
        .contains("*     <name>")
        .contains("--tags=<tag>[,...]")
        .doesNotContain("* --repository-uri")
        .doesNotContain("* --repository-branch");
  }
}
