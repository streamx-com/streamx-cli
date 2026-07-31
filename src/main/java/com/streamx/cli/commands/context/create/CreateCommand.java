package com.streamx.cli.commands.context.create;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.ContextNameCompletionCandidates;
import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import picocli.CommandLine;

@CommandLine.Command(
    name = "create",
    header = "Create a context and switch to it",
    description = "The new context starts with empty settings and becomes the current context."
)
public class CreateCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(index = "0", description = "Context name (lowercase, digits, dashes)")
  public String name;

  @CommandLine.Option(
      names = "--from",
      description = "Copy settings and event templates (not the login) from this context",
      completionCandidates = ContextNameCompletionCandidates.class
  )
  public String from;

  @Override
  public boolean needsContext() {
    return false;
  }

  @Override
  public CommandResult<Void> runCommand() {
    StreamxHome.requireValidContextName(name);
    if (StreamxHome.contextExists(name)) {
      throw new CliException(msg.contextAlreadyExists(name));
    }
    if (from != null) {
      StreamxHome.requireValidContextName(from);
      if (!StreamxHome.contextExists(from)) {
        throw new CliException(msg.contextNotFound(from));
      }
    }

    try {
      Files.createDirectories(StreamxHome.getConfigDirOf(name));
      Files.createDirectories(StreamxHome.getEventTemplatesDirOf(name));
      if (from != null) {
        Path source = StreamxHome.getConfigDirOf(from).resolve("application.properties");
        if (Files.isRegularFile(source)) {
          Files.copy(source, StreamxHome.getConfigDirOf(name).resolve("application.properties"));
        }
        copyTree(
            StreamxHome.getEventTemplatesDirOf(from),
            StreamxHome.getEventTemplatesDirOf(name));
      }
    } catch (IOException e) {
      throw new CliException(msg.contextCreateFailed(name, e.getMessage()), e);
    }

    System.out.println(msg.contextCreated(name));
    try {
      StreamxHome.writeCurrentContextPointer(name);
      System.out.println(msg.contextSwitched(name));
    } catch (IOException e) {
      throw new CliException(msg.contextSwitchFailed(e.getMessage()), e);
    }
    System.err.println(msg.contextCreateConfigureHint());
    return new CommandResult<>(null);
  }

  private static void copyTree(Path source, Path target) throws IOException {
    if (!Files.isDirectory(source)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(source)) {
      for (Path path : paths.toList()) {
        Path destination = target.resolve(source.relativize(path).toString());
        if (Files.isDirectory(path)) {
          Files.createDirectories(destination);
        } else {
          Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }
}
