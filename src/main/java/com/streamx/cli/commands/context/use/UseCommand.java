package com.streamx.cli.commands.context.use;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.ContextNameCompletionCandidates;
import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import java.io.IOException;
import picocli.CommandLine;

@CommandLine.Command(
    name = "use",
    header = "Switch the current context"
)
public class UseCommand extends AbstractSilentCommand {

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
      throw new CliException(msg.contextNotFound(name));
    }
    try {
      StreamxHome.writeCurrentContextPointer(name);
    } catch (IOException e) {
      throw new CliException(msg.contextSwitchFailed(e.getMessage()), e);
    }
    System.out.println(msg.contextSwitched(name));
    return new CommandResult<>(null);
  }
}
