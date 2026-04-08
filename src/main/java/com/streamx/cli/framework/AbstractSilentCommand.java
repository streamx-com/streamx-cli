package com.streamx.cli.framework;

import java.util.List;

public abstract class AbstractSilentCommand extends AbstractCommand<Void> {
  @Override
  public List<String> getHiddenOptions() {
    return List.of(CommonOptions.OUTPUT_LONG);
  }

  @Override
  public String getTextOutput(CommandResult<Void> result) {
    return "";
  }
}
