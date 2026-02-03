package com.streamx.cli.commands.settings.set;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.DotStreamxConfigSource;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import picocli.CommandLine;

@CommandLine.Command(
    name = "set",
    mixinStandardHelpOptions = true,
    description = "Set configuration property"
)
public class SetCommand extends AbstractSilentCommand {
  @CommandLine.Parameters(index = "0", description = "Property key")
  private String key;

  @CommandLine.Parameters(index = "1", description = "Property value")
  private String value;

  @Override
  public CommandResult<Void> runCommand() throws RuntimeException {
    var url = DotStreamxConfigSource.getUrl();
    var path = Paths.get(url.getPath());

    Properties properties = new Properties();

    try (var inputStream = url.openStream()) {
      properties.load(inputStream);
    } catch (IOException e) {
      throw new RuntimeException(msg.unableToSetSettingsProperty(), e);
    }

    properties.setProperty(key, value);

    try (var outputStream = Files.newOutputStream(path)) {
      properties.store(outputStream, null);
    } catch (IOException e) {
      throw new RuntimeException(msg.unableToSetSettingsProperty(), e);
    }

    return new CommandResult<>(null);
  }
}