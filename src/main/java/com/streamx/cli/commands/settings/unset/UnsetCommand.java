
package com.streamx.cli.commands.settings.unset;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.StreamxHome;
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
    name = "unset",
    header = "Unset settings property"
)
public class UnsetCommand extends AbstractSilentCommand {
  @CommandLine.Parameters(index = "0", description = "Property key")
  public String key;

  @Override
  public CommandResult<Void> runCommand() throws RuntimeException {
    URL url = StreamxHome.getConfigUrl();
    Path path = Paths.get(url.getPath());

    Properties properties = new Properties();

    try (InputStream inputStream = url.openStream()) {
      properties.load(inputStream);
    } catch (IOException e) {
      throw new CliException(msg.unableToUnsetSettingsProperty(key, e.getMessage()), e);
    }

    properties.remove(key);

    try (OutputStream outputStream = Files.newOutputStream(path)) {
      properties.store(outputStream, null);
    } catch (IOException e) {
      throw new CliException(msg.unableToUnsetSettingsProperty(key, e.getMessage()), e);
    }

    return new CommandResult<>(null);
  }
}