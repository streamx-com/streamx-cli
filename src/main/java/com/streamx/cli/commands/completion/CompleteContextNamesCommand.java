package com.streamx.cli.commands.completion;

import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "__complete-context-names",
    hidden = true,
    header = "Internal: list every context name, one per line"
)
public class CompleteContextNamesCommand extends AbstractCommand<List<String>> {

  @Override
  public boolean needsContext() {
    return false;
  }

  @Override
  public CommandResult<List<String>> runCommand() {
    return new CommandResult<>(StreamxHome.listContextNames());
  }

  @Override
  public String getTextOutput(CommandResult<List<String>> result) {
    return String.join("\n", result.getData());
  }
}
