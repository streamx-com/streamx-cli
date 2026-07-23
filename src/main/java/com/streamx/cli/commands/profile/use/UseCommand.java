package com.streamx.cli.commands.profile.use;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.ProfileNameCompletionCandidates;
import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import java.io.IOException;
import picocli.CommandLine;

@CommandLine.Command(
    name = "use",
    header = "Switch the current profile"
)
public class UseCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(
      index = "0",
      description = "Profile name",
      completionCandidates = ProfileNameCompletionCandidates.class
  )
  public String name;

  @Override
  public boolean needsProfile() {
    return false;
  }

  @Override
  public CommandResult<Void> runCommand() {
    StreamxHome.requireValidProfileName(name);
    if (!StreamxHome.profileExists(name)) {
      throw new CliException(msg.profileNotFound(name));
    }
    try {
      StreamxHome.writeCurrentProfilePointer(name);
    } catch (IOException e) {
      throw new CliException(msg.profileSwitchFailed(e.getMessage()), e);
    }
    System.out.println(msg.profileSwitched(name));
    return new CommandResult<>(null);
  }
}
