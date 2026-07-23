package com.streamx.cli.commands.completion;

import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "__complete-profile-names",
    hidden = true,
    header = "Internal: list every profile name, one per line"
)
public class CompleteProfileNamesCommand extends AbstractCommand<List<String>> {

  @Override
  public boolean needsProfile() {
    return false;
  }

  @Override
  public CommandResult<List<String>> runCommand() {
    return new CommandResult<>(StreamxHome.listProfileNames());
  }

  @Override
  public String getTextOutput(CommandResult<List<String>> result) {
    return String.join("\n", result.getData());
  }
}
