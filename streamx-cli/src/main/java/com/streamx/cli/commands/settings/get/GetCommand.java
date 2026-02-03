package com.streamx.cli.commands.settings.get;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.DotStreamxConfigSource;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import java.io.IOException;
import java.util.Properties;
import picocli.CommandLine;

@CommandLine.Command(
    name = "get",
    mixinStandardHelpOptions = true,
    description = "Get configuration property"
)
public class GetCommand extends AbstractCommand<GetCommandResult> {
  @CommandLine.Parameters(index = "0", description = "Property key")
  private String key;

  @Override
  public String getTextOutput(CommandResult<GetCommandResult> result) throws RuntimeException {
    return result.result.value();
  }

  @Override
  public CommandResult<GetCommandResult> runCommand() throws RuntimeException {
    var url = DotStreamxConfigSource.getUrl();

    try (var inputStream = url.openStream()) {
      Properties properties = new Properties();
      properties.load(inputStream);

      var value = properties.getProperty(key);
      if (value == null) {
        throw new RuntimeException(msg.noSettingsPropertyFound(key));
      }

      var result = new GetCommandResult(key, value);

      return new CommandResult<>(result);
    } catch (IOException e) {
      throw new RuntimeException(msg.unableToGetSettingsProperty(), e);
    }
  }
}