package com.streamx.cli.commands.publish.event;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.ingestion.CloudEventsSerde;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.jetbrains.annotations.NotNull;

class EventTemplateLoader {

  private static final String DEFAULT_TEMPLATES_DIR = "default-event-templates";
  private static final String DEFAULT_TEMPLATE_PREFIX = "com.streamx.blueprints.";

  private static final String TEMPLATE_EXTENSION = ".json";
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
      JsonNode jsonNode = mapper.readTree(template);
      CloudEventsSerde.fromJson(jsonNode);
    } catch (Exception e) {
      throw new CliException(msg.invalidEventTemplate(eventType));
    }
  }

  private TemplateDescriptor findTemplate(@NotNull String eventType) {
    TemplateDescriptor defaultTemplate = findDefaultTemplate(eventType);
    if (defaultTemplate != null) {
      return defaultTemplate;
    }

    if (!eventType.startsWith(DEFAULT_TEMPLATE_PREFIX)) {
      defaultTemplate = findDefaultTemplate(DEFAULT_TEMPLATE_PREFIX + eventType);
      if (defaultTemplate != null) {
        return defaultTemplate;
      }
    }

    TemplateDescriptor templateFromSettings = findTemplateInSettings(eventType);
    if (templateFromSettings != null) {
      return templateFromSettings;
    }

    throw new CliException(msg.eventTemplateNotFound(eventType));
  }

  private TemplateDescriptor findDefaultTemplate(String templateName) {
    String resourcePath = "/" + DEFAULT_TEMPLATES_DIR + "/" + templateName + TEMPLATE_EXTENSION;
    try (InputStream inputStream = EventTemplateLoader.class.getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        return null;
      }
      return new TemplateDescriptor(
          new String(inputStream.readAllBytes(), StandardCharsets.UTF_8),
          resourcePath
      );
    } catch (IOException e) {
      return null;
    }
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
        Path configDir = Paths.get(url.toURI()).getParent();
        path = configDir.resolve(path);
      }

      if (Files.exists(path) && Files.isRegularFile(path)) {
        return new TemplateDescriptor(
            Files.readString(path),
            path.toAbsolutePath().toString()
        );
      }

      return null;
    } catch (CliException e) {
      throw e;
    } catch (Exception e) {
      throw new CliException(msg.unableToGetSettingsProperty(), e);
    }
  }
}