package com.streamx.cli.commands.settings.eventtemplates.register;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.publish.event.EventTemplateLoader;
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
    name = "register",
    header = "Register an event template file under a template ID (writes to settings)",
    description = "Adds an `eventtemplate.<id>=<path>` entry to the active context's "
        + "application.properties. The path can be absolute or relative to the context "
        + "directory. Registered templates take precedence over the context's custom "
        + "templates and the shared default templates.",
    footer = {
        "",
        "Examples:",
        "  streamx settings event-templates register my.alias /abs/path/to/template.json",
        "  streamx settings event-templates register my.alias relative/path.json"
    }
)
public class RegisterCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(
      index = "0",
      description = "Template ID (the value passed to `publish event`)"
  )
  public String templateId;

  @CommandLine.Parameters(
      index = "1",
      description = "Path to the template JSON file (relative to the context directory "
          + "or absolute)"
  )
  public String path;

  @Override
  public CommandResult<Void> runCommand() throws RuntimeException {
    URL url = StreamxHome.getConfigUrl();
    Path configPath = Paths.get(url.getPath());

    Properties properties = new Properties();
    try (InputStream inputStream = url.openStream()) {
      properties.load(inputStream);
    } catch (IOException e) {
      throw new CliException(msg.unableToSetSettingsProperty(), e);
    }

    properties.setProperty(
        EventTemplateLoader.TEMPLATE_SETTINGS_MAPPING_PREFIX + templateId,
        path
    );

    try (OutputStream outputStream = Files.newOutputStream(configPath)) {
      properties.store(outputStream, null);
    } catch (IOException e) {
      throw new CliException(msg.unableToSetSettingsProperty(), e);
    }

    return new CommandResult<>(null);
  }
}
