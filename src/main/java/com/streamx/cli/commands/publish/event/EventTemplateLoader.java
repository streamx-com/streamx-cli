package com.streamx.cli.commands.publish.event;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.DotStreamxConfigSource;
import com.streamx.cli.framework.CliException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

class EventTemplateLoader {

  private static final String DEFAULT_TEMPLATES_DIR = "default-event-templates";
  private static final String TEMPLATE_EXTENSION = ".json";
  private static final String BLUEPRINTS_PREFIX = "com.streamx.blueprints.";
  private static final String TEMPLATE_SETTINGS_MAPPING_PREFIX = "eventtemplate.";

  private static final List<String> KNOWN_TEMPLATES = List.of(
      "com.streamx.blueprints.page.published.v1",
      "com.streamx.blueprints.page.unpublished.v1"
  );

  private final Map<String, String> knownTemplates;

  EventTemplateLoader() {
    this.knownTemplates = buildKnownTemplates();
  }

  private static Map<String, String> buildKnownTemplates() {
    Map<String, String> map = new HashMap<>();
    for (String fullType : KNOWN_TEMPLATES) {
      map.put(fullType, fullType);
      if (fullType.startsWith(BLUEPRINTS_PREFIX)) {
        String shortType = fullType.substring(BLUEPRINTS_PREFIX.length());
        map.put(shortType, fullType);
      }
    }
    return Map.copyOf(map);
  }

  String load(@NotNull String eventType) {
    String templateFromSettingsMapping = getTemplateFromSettingsMapping(eventType);

    if (templateFromSettingsMapping != null) {
      return templateFromSettingsMapping;
    }

    // Fallback to well-known templates
    String templateName = knownTemplates.get(eventType);
    if (templateName != null) {
      String resourcePath = DEFAULT_TEMPLATES_DIR + templateName + TEMPLATE_EXTENSION;
      try (InputStream inputStream = EventTemplateLoader.class.getResourceAsStream(resourcePath)) {
        if (inputStream == null) {
          throw new CliException(msg.eventTemplateNotFound(eventType));
        }

        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new CliException(msg.eventTemplateNotFound(eventType), e);
      }
    }

    throw new CliException(msg.eventTemplateNotFound(eventType));
  }

  private String getTemplateFromSettingsMapping(String eventType) {
    URL url = DotStreamxConfigSource.getUrl();

    try (InputStream inputStream = url.openStream()) {
      Properties properties = new Properties();
      properties.load(inputStream);

      String key = TEMPLATE_SETTINGS_MAPPING_PREFIX + eventType;

      String pathAsString = properties.getProperty(key);
      if (pathAsString == null) {
        return null;
      }

      Path path = Paths.get(pathAsString);
      if (Files.exists(path) && Files.isRegularFile(path)) {
        return Files.readString(path);
      }

      return null;
    } catch (Exception e) {
      throw new CliException(msg.unableToGetSettingsProperty(), e);
    }
  }
}