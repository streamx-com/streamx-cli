package com.streamx.cli.commands.context.current;

import com.streamx.cli.config.Contexts;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import picocli.CommandLine;

@CommandLine.Command(
    name = "current",
    header = "Print the active context name"
)
public class CurrentCommand extends AbstractCommand<String> {

  @Override
  public boolean needsContext() {
    return false;
  }

  @Override
  public CommandResult<String> runCommand() {
    return new CommandResult<>(Contexts.getActiveContext());
  }

  @Override
  public String getTextOutput(CommandResult<String> result) {
    return result.getData();
  }
}
