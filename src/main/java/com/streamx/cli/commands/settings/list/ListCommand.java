package com.streamx.cli.commands.settings.list;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.DotStreamxConfigSource;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    mixinStandardHelpOptions = true,
    header = "Display all settings properties"
)
public class ListCommand extends AbstractCommand<Map<String, String>> {
  @Override
  public String getTextOutput(CommandResult<Map<String, String>> result) {
    if (result.getData().isEmpty()) {
      return msg.listSettingsNoPropertiesFound();
    }

    StringBuilder stringOutput = new StringBuilder();

    Map<String, String> sortedProperties = new TreeMap<>(result.getData());

    int maxKeyLength = sortedProperties.keySet().stream()
        .mapToInt(String::length)
        .max()
        .orElse(0);

    stringOutput.append(msg.listSettingsHeader()).append("\n");

    for (Map.Entry<String, String> entry : sortedProperties.entrySet()) {
      String paddedKey = String.format("%-" + maxKeyLength + "s", entry.getKey());
      stringOutput.append(paddedKey).append(" =");
      if (!entry.getValue().isEmpty()) {
        stringOutput.append(" ").append(entry.getValue());
      }
      stringOutput.append("\n");
    }

    return stringOutput.toString().strip();
  }

  @Override
  public CommandResult<Map<String, String>> runCommand() {
    URL url = DotStreamxConfigSource.getUrl();
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
