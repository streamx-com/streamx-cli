package com.streamx.cli.commands.profile.list;

import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.TextTable;
import com.streamx.cli.platform.PlatformConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    header = "List profiles"
)
public class ListCommand extends AbstractCommand<List<ProfileInfo>> {

  @CommandLine.Option(
      names = {"-q", "--quiet"},
      description = "Only display profile names, one per line (for piping to xargs)"
  )
  public boolean quiet;

  @Override
  public boolean needsProfile() {
    return false;
  }

  @Override
  public String getTextOutput(CommandResult<List<ProfileInfo>> result) {
    List<ProfileInfo> profiles = result.getData();
    if (quiet) {
      return profiles.stream().map(ProfileInfo::name).collect(Collectors.joining("\n"));
    }
    return TextTable.render(
        List.of("NAME", "ACTIVE", "PLATFORM URL", "LOGGED IN"),
        profiles.stream()
            .map(profile -> List.of(
                profile.name(),
                profile.active() ? "*" : "",
                profile.platformUrl() == null ? "-" : profile.platformUrl(),
                profile.loggedIn() ? "yes" : "-"))
            .toList());
  }

  @Override
  public CommandResult<List<ProfileInfo>> runCommand() {
    String active = StreamxHome.getActiveProfile();
    List<ProfileInfo> profiles = StreamxHome.listProfileNames().stream()
        .map(name -> describe(name, name.equals(active)))
        .toList();
    return new CommandResult<>(profiles);
  }

  private static ProfileInfo describe(String name, boolean active) {
    Path configDir = StreamxHome.getConfigDirOf(name);
    String platformUrl = null;
    Path settings = configDir.resolve("application.properties");
    if (Files.isRegularFile(settings)) {
      Properties properties = new Properties();
      try (InputStream in = Files.newInputStream(settings)) {
        properties.load(in);
        platformUrl = properties.getProperty(PlatformConfig.STREAMX_PLATFORM_URL);
      } catch (IOException expected) {
      }
    }
    boolean loggedIn = Files.isRegularFile(configDir.resolve("credentials.json"));
    return new ProfileInfo(name, active, platformUrl, loggedIn);
  }
}
