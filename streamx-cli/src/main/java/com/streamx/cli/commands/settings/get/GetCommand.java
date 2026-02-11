package com.streamx.cli.commands.settings.get;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.DotStreamxConfigSource;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import picocli.CommandLine;

@CommandLine.Command(
    name = "get",
    mixinStandardHelpOptions = true,
    description = "Get configuration property"
)
public class GetCommand extends AbstractCommand<String> {
  @CommandLine.Parameters(index = "0", description = "Property key")
  public String key;

  @Override
  public String getTextOutput(CommandResult<String> result) {
    return result.getData();
  }

  @Override
  public CommandResult<String> runCommand() {
    URL url = DotStreamxConfigSource.getUrl();

    try (InputStream inputStream = url.openStream()) {
      Properties properties = new Properties();
      properties.load(inputStream);

      String value = properties.getProperty(key);
      if (value == null) {
        throw new CliException(msg.noSettingsPropertyFound(key));
      }

      return new CommandResult<>(value);
    } catch (IOException e) {
      throw new CliException(msg.unableToGetSettingsProperty(), e);
    }
  }
}