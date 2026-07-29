package com.streamx.cli.commands.context.list;

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
    header = "List contexts"
)
public class ListCommand extends AbstractCommand<List<ContextInfo>> {

  @CommandLine.Option(
      names = {"-q", "--quiet"},
      description = "Only display context names, one per line (for piping to xargs)"
  )
  public boolean quiet;

  @Override
  public boolean needsContext() {
    return false;
  }

  @Override
  public String getTextOutput(CommandResult<List<ContextInfo>> result) {
    List<ContextInfo> contexts = result.getData();
    if (quiet) {
      return contexts.stream().map(ContextInfo::name).collect(Collectors.joining("\n"));
    }
    return TextTable.render(
        List.of("NAME", "ACTIVE", "PLATFORM URL", "LOGGED IN"),
        contexts.stream()
            .map(context -> List.of(
                context.name(),
                context.active() ? "*" : "",
                context.platformUrl() == null ? "-" : context.platformUrl(),
                context.loggedIn() ? "yes" : "-"))
            .toList());
  }

  @Override
  public CommandResult<List<ContextInfo>> runCommand() {
    String active = StreamxHome.getActiveContext();
    List<ContextInfo> contexts = StreamxHome.listContextNames().stream()
        .map(name -> describe(name, name.equals(active)))
        .toList();
    return new CommandResult<>(contexts);
  }

  private static ContextInfo describe(String name, boolean active) {
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
    return new ContextInfo(name, active, platformUrl, loggedIn);
  }
}
