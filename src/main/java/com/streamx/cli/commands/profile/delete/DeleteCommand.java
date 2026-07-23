package com.streamx.cli.commands.profile.delete;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.ProfileNameCompletionCandidates;
import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import picocli.CommandLine;

@CommandLine.Command(
    name = "delete",
    header = "Delete a profile",
    description = "Removes the profile's settings, event templates and stored login from this "
        + "machine. The login is not revoked; run 'streamx auth logout' in the profile first."
)
public class DeleteCommand extends AbstractSilentCommand {

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
      throw new CliException(msg.profileDoesNotExist(name));
    }
    if (name.equals(StreamxHome.getActiveProfile())) {
      throw new CliException(msg.profileCannotDeleteActive(name));
    }
    if (name.equals(StreamxHome.readCurrentProfilePointer())) {
      throw new CliException(msg.profileCannotDeleteCurrent(name));
    }

    Path profileDir = StreamxHome.getProfileDirOf(name);
    boolean hadLogin =
        Files.isRegularFile(StreamxHome.getConfigDirOf(name).resolve("credentials.json"));
    try (Stream<Path> paths = Files.walk(profileDir)) {
      paths.sorted(Comparator.reverseOrder()).forEach(path -> {
        try {
          Files.delete(path);
        } catch (IOException e) {
          throw new CliException(msg.profileDeleteFailed(name, e.getMessage()), e);
        }
      });
    } catch (IOException e) {
      throw new CliException(msg.profileDeleteFailed(name, e.getMessage()), e);
    }

    System.out.println(msg.profileDeleted(name));
    if (hadLogin) {
      System.err.println(msg.profileDeletedLoginNote());
    }
    return new CommandResult<>(null);
  }
}
