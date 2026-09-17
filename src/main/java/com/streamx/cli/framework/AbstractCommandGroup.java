package com.streamx.cli.framework;

import java.util.List;

public class AbstractCommandGroup extends AbstractCommand<Void> {
  @Override
  public CommandResult<Void> runCommand() {
    this.printUsage();
    return new CommandResult<>(null);
  }

  @Override
  public boolean needsContext() {
    return false;
  }

  @Override
  public String getTextOutput(CommandResult<Void> result) {
    return "";
  }

  @Override
  public List<String> getHiddenOptions() {
    return List.of(
      CommonOptions.OUTPUT_LONG,
      CommonOptions.VERBOSE_LONG
    );
  }
}
