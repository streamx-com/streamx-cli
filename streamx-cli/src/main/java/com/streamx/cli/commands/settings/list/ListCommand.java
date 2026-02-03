package com.streamx.cli.commands.settings.list;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.DotStreamxConfigSource;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    mixinStandardHelpOptions = true,
    description = "Display configuration properties"
)
public class ListCommand extends AbstractCommand<List<Property>> {
  @Override
  public String getTextOutput(CommandResult<List<Property>> result) throws RuntimeException {
    StringBuilder stringOutput = new StringBuilder();

    Map<String, String> map = result.result.stream()
        .collect(Collectors.toMap(Property::key, Property::value));

    Map<String, String> sortedProperties = new TreeMap<>(map);

    int maxKeyLength = sortedProperties.keySet().stream()
        .mapToInt(String::length)
        .max()
        .orElse(0);

    stringOutput.append("\nConfiguration properties:\n");
    String repeat = "=".repeat(Math.min(80, maxKeyLength + 40));
    stringOutput.append(repeat).append("\n");

    for (Map.Entry<String, String> entry : sortedProperties.entrySet()) {
      String paddedKey = String.format("%-" + maxKeyLength + "s", entry.getKey());
      stringOutput.append(paddedKey).append(" = ").append(entry.getValue()).append("\n");
    }

    stringOutput.append(repeat).append("\n");
    stringOutput.append("Total properties: ").append(result.result.size()).append("\n");

    return stringOutput.toString();
  }

  @Override
  public CommandResult<List<Property>> runCommand() throws RuntimeException {
    var url = DotStreamxConfigSource.getUrl();
    var properties = getProperties(url);

    return new CommandResult<>(properties);
  }

  private List<Property> getProperties(URL url) throws RuntimeException {
    try (var input = url.openStream()) {
      Properties properties = new Properties();
      properties.load(input);

      return properties.stringPropertyNames().stream()
          .map(key -> new Property(key, properties.getProperty(key)))
          .toList();
    } catch (Exception e) {
      throw new RuntimeException(msg.failedToLoadPropertiesFrom(url.getPath()), e);
    }
  }
}
