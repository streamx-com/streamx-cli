package com.streamx.cli.commands.context.delete;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.ContextNameCompletionCandidates;
import com.streamx.cli.config.Contexts;
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
    Contexts.requireValidContextName(name);
    if (!Contexts.contextExists(name)) {
      throw new CliException(msg.contextNotFound(name, name));
    }
    final boolean wasActive = name.equals(Contexts.getActiveContext());
    final boolean wasCurrent = name.equals(Contexts.readCurrentContextPointer());

    Path contextDir = Contexts.getContextDirOf(name);
    final boolean hadLogin =
        Files.isRegularFile(Contexts.getConfigDirOf(name).resolve("credentials.json"));
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

    if (wasCurrent) {
      try {
        Contexts.clearCurrentContextPointer();
      } catch (IOException e) {
        throw new CliException(msg.contextDeleteFailed(name, e.getMessage()), e);
      }
    }

    System.err.println(msg.contextDeleted(name));
    if (hadLogin) {
      System.err.println(msg.contextDeletedLoginNote());
    }
    if (wasActive) {
      System.err.println(msg.contextDeletedWasActive(Contexts.getActiveContextSource()));
    }
    if (wasCurrent) {
      System.err.println(msg.contextDeletedWasCurrent());
    }
    return new CommandResult<>(null);
  }
}
