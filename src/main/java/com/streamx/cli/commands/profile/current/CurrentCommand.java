package com.streamx.cli.commands.profile.current;

import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import picocli.CommandLine;

@CommandLine.Command(
    name = "current",
    header = "Print the active profile name"
)
public class CurrentCommand extends AbstractSilentCommand {

  @Override
  public boolean needsProfile() {
    return false;
  }

  @Override
  public CommandResult<Void> runCommand() {
    System.out.println(StreamxHome.getActiveProfile());
    return new CommandResult<>(null);
  }
}
