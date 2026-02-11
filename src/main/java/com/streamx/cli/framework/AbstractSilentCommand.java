package com.streamx.cli.framework;

import java.util.List;

// Extend this class for commands that don't produce any output, e.g.: settings set.
public abstract class AbstractSilentCommand extends AbstractCommand<Void> {
  @Override
  public List<String> getHiddenOptions() {
    return List.of(CommonOption.OUTPUT_LONG);
  }

  @Override
  public String getTextOutput(CommandResult<Void> result) {
    return "";
  }
}
