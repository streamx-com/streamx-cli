package com.streamx.cli.framework;

import java.util.List;

// Extend this class for commands that don't do anything except display their subcommands.
public class AbstractCommandGroup extends AbstractCommand<Void> {
  @Override
  public CommandResult<Void> runCommand() {
    this.printUsage();
    return new CommandResult<>(null);
  }

  @Override
  public String getTextOutput(CommandResult<Void> result) {
    return "";
  }

  @Override
  public List<String> getHiddenOptions() {
    return List.of(
      CommonOption.OUTPUT_LONG,
      CommonOption.VERBOSE_LONG
    );
  }
}
