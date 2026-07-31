package com.streamx.cli.commands.context.delete;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.ContextNameCompletionCandidates;
import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import picocli.CommandLine;

@CommandLine.Command(
    name = "delete",
    header = "Delete a context",
    description = "Removes the context's settings, event templates and stored login from this "
        + "machine. The login is not revoked; run 'streamx auth logout' in the context first."
)
public class DeleteCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(
      index = "0",
      description = "Context name",
      completionCandidates = ContextNameCompletionCandidates.class
  )
  public String name;

  @Override
  public boolean needsContext() {
    return false;
  }

  @Override
  public CommandResult<Void> runCommand() {
    StreamxHome.requireValidContextName(name);
    if (!StreamxHome.contextExists(name)) {
      throw new CliException(msg.contextDoesNotExist(name));
    }
    if (name.equals(StreamxHome.getActiveContext())) {
      throw new CliException(msg.contextCannotDeleteActive(name));
    }
    if (name.equals(StreamxHome.readCurrentContextPointer())) {
      throw new CliException(msg.contextCannotDeleteCurrent(name));
    }

    Path contextDir = StreamxHome.getContextDirOf(name);
    boolean hadLogin =
        Files.isRegularFile(StreamxHome.getConfigDirOf(name).resolve("credentials.json"));
    try (Stream<Path> paths = Files.walk(contextDir)) {
      paths.sorted(Comparator.reverseOrder()).forEach(path -> {
        try {
          Files.delete(path);
        } catch (IOException e) {
          throw new CliException(msg.contextDeleteFailed(name, e.getMessage()), e);
        }
      });
    } catch (IOException e) {
      throw new CliException(msg.contextDeleteFailed(name, e.getMessage()), e);
    }

    System.out.println(msg.contextDeleted(name));
    if (hadLogin) {
      System.err.println(msg.contextDeletedLoginNote());
    }
    return new CommandResult<>(null);
  }
}
