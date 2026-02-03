package com.streamx.cli.commands.settings.set;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.DotStreamxConfigSource;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
  public String key;

  @CommandLine.Parameters(index = "1", description = "Property value")
  public String value;

  @Override
  public CommandResult<Void> runCommand() throws RuntimeException {
    URL url = DotStreamxConfigSource.getUrl();
    Path path = Paths.get(url.getPath());

    Properties properties = new Properties();

    try (InputStream inputStream = url.openStream()) {
      properties.load(inputStream);
    } catch (IOException e) {
      throw new CliException(msg.unableToSetSettingsProperty(), e);
    }

    properties.setProperty(key, value);

    try (OutputStream outputStream = Files.newOutputStream(path)) {
      properties.store(outputStream, null);
    } catch (IOException e) {
      throw new CliException(msg.unableToSetSettingsProperty(), e);
    }

    return new CommandResult<>(null);
  }
}