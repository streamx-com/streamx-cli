package com.streamx.cli.framework.testing;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import java.util.List;
import java.util.function.Supplier;

// Helper class for testing SilentAbstractCommand
public class AbstractSilentTestCommand extends AbstractSilentCommand {
  public Supplier<CommandResult<Void>> runCommandHandler;
  public Supplier<List<String>> hiddenOptionsHandler;

  public void setRunCommandHandler(Supplier<CommandResult<Void>> handler) {
    this.runCommandHandler = handler;
  }

  public void setHiddenOptionsHandler(Supplier<List<String>> handler) {
    this.hiddenOptionsHandler = handler;
  }

  @Override
  public CommandResult<Void> runCommand() throws CliException {
    if (runCommandHandler != null) {
      return runCommandHandler.get();
    }
    throw new CliException("No run command handler set");
  }

  @Override
  public List<String> getHiddenOptions() {
    if (hiddenOptionsHandler != null) {
      return hiddenOptionsHandler.get();
    }
    return super.getHiddenOptions();
  }
}
