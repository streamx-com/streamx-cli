package com.streamx.cli.framework;

import java.util.List;

public class AbstractCommandGroup extends AbstractCommand<Void> {
  @Override
  public CommandResult<Void> runCommand() {
    this.printUsage();
    return new CommandResult<>(null);
  }

  /** Groups only print usage; that must work even when the selected profile is broken. */
  @Override
  public boolean needsProfile() {
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
