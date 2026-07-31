package com.streamx.cli.commands.context.current;

import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import picocli.CommandLine;

@CommandLine.Command(
    name = "current",
    header = "Print the active context name"
)
public class CurrentCommand extends AbstractSilentCommand {

  @Override
  public boolean needsContext() {
    return false;
  }

  @Override
  public CommandResult<Void> runCommand() {
    System.out.println(StreamxHome.getActiveContext());
    return new CommandResult<>(null);
  }
}
