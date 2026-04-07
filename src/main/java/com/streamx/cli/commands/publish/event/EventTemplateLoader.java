package com.streamx.cli.commands.publish.event;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.CliException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.jetbrains.annotations.NotNull;

class EventTemplateLoader {

  private static final String TEMPLATE_SETTINGS_MAPPING_PREFIX = "eventtemplate.";

  record TemplateDescriptor(String template, String templatePath) {}

  public TemplateDescriptor load(@NotNull String eventType) {
    TemplateDescriptor templateDescriptor = findTemplate(eventType);
    validateTemplate(templateDescriptor.template, eventType);
    return templateDescriptor;
  }

  private void validateTemplate(String template, String eventType) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.readTree(template);
    } catch (Exception e) {
      throw new CliException(msg.invalidEventTemplate(eventType));
    }
  }

  private TemplateDescriptor findTemplate(@NotNull String eventType) {
    TemplateDescriptor templateFromSettings = findTemplateInSettings(eventType);
    if (templateFromSettings != null) {
      return templateFromSettings;
    }

    TemplateDescriptor defaultTemplate = findDefaultTemplate(eventType);
    if (defaultTemplate != null) {
      return defaultTemplate;
    }

    throw new CliException(msg.eventTemplateNotFound(eventType));
  }

  private TemplateDescriptor findDefaultTemplate(String templateName) {
    Path onDisk = DefaultEventTemplates.resolveOnDisk(templateName);
    if (Files.isRegularFile(onDisk)) {
      try {
        return new TemplateDescriptor(
            Files.readString(onDisk),
            onDisk.toAbsolutePath().toString()
        );
      } catch (IOException ignored) {
        // fall back to embedded
      }
    }

    String embedded = DefaultEventTemplates.loadEmbedded(templateName);
    if (embedded == null) {
      return null;
    }
    return new TemplateDescriptor(
        embedded,
        "/" + DefaultEventTemplates.DIRECTORY + "/" + templateName + DefaultEventTemplates.EXTENSION
    );
  }

  private TemplateDescriptor findTemplateInSettings(String eventType) {
    URL url = StreamxHome.getConfigUrl();

    try (InputStream inputStream = url.openStream()) {
      Properties properties = new Properties();
      properties.load(inputStream);

      String key = TEMPLATE_SETTINGS_MAPPING_PREFIX + eventType;

      String pathAsString = properties.getProperty(key);
      if (pathAsString == null) {
        return null;
      }

      Path path = Paths.get(pathAsString);
      if (!path.isAbsolute()) {
        path = StreamxHome.getStreamxHome().resolve(path);
      }

      if (Files.exists(path) && Files.isRegularFile(path)) {
        return new TemplateDescriptor(
            Files.readString(path),
            path.toAbsolutePath().toString()
        );
      }

      return null;
    } catch (Exception e) {
      throw new CliException(msg.unableToGetSettingsProperty(e.getMessage()), e);
    }
  }
}