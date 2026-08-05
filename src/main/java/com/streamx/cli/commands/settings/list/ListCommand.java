package com.streamx.cli.commands.settings.list;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.TextTable;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    header = "Display all settings properties"
)
public class ListCommand extends AbstractCommand<Map<String, String>> {
  @Override
  public String getTextOutput(CommandResult<Map<String, String>> result) {
    if (result.getData().isEmpty()) {
      return msg.listSettingsNoPropertiesFound();
    }

    Map<String, String> sortedProperties = new TreeMap<>(result.getData());
    return TextTable.render(
        List.of("KEY", "VALUE"),
        sortedProperties.entrySet().stream()
            .map(entry -> List.of(
                entry.getKey(),
                entry.getValue().isEmpty() ? "-" : entry.getValue()))
            .toList());
  }

  @Override
  public CommandResult<Map<String, String>> runCommand() {
    URL url = StreamxHome.getConfigUrl();
    Map<String, String> properties = getProperties(url);

    return new CommandResult<>(properties);
  }

  private Map<String, String> getProperties(URL url) {
    try (InputStream input = url.openStream()) {
      Properties properties = new Properties();
      properties.load(input);

      return properties.stringPropertyNames().stream()
          .collect(Collectors.toMap(
              key -> key,
              properties::getProperty
          ));
    } catch (Exception e) {
      throw new CliException(msg.failedToLoadPropertiesFrom(url.getPath()), e);
    }
  }
}
