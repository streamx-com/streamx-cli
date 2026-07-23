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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class EventTemplateCatalog {

  public static final String SOURCE_SETTINGS = "settings";

  public static final String SOURCE_CUSTOM = "custom";

  public static final String SOURCE_DEFAULT = "default";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private EventTemplateCatalog() {
  }

  public record TemplateLocation(
      String id,
      String type,
      String path,
      String source
  ) {
  }

  public static List<TemplateLocation> listAll() {
    Map<String, TemplateLocation> byId = new TreeMap<>();
    addFromDefaultsFolder(byId);
    addFromUserFolder(byId);
    addFromSettings(byId);
    return List.copyOf(byId.values());
  }

  public static List<String> templateIds() {
    return listAll().stream().map(TemplateLocation::id).toList();
  }

  public static Optional<TemplateLocation> findById(String id) {
    if (id == null || id.isBlank()) {
      return Optional.empty();
    }
    return listAll().stream().filter(t -> t.id().equals(id)).findFirst();
  }

  public static Map<String, String> listSettingsRegistrations() {
    Map<String, String> result = new TreeMap<>();
    readSettingsEntries().forEach((id, path) -> result.put(id, path.toAbsolutePath().toString()));
    return result;
  }

  public static Map<String, Path> readSettingsEntries() {
    Properties properties = loadConfig();
    if (properties == null) {
      return Map.of();
    }
    Map<String, Path> result = new TreeMap<>();
    for (String key : properties.stringPropertyNames()) {
      if (!key.startsWith(EventTemplateLoader.TEMPLATE_SETTINGS_MAPPING_PREFIX)) {
        continue;
      }
      String id = key.substring(EventTemplateLoader.TEMPLATE_SETTINGS_MAPPING_PREFIX.length());
      String value = properties.getProperty(key);
      if (value == null || value.isBlank()) {
        continue;
      }
      result.put(id, resolveRelativeToProfileDir(value));
    }
    return result;
  }

  public static Path resolveRelativeToProfileDir(String pathAsString) {
    Path path = Paths.get(pathAsString);
    if (!path.isAbsolute()) {
      path = StreamxHome.getProfileDir().resolve(path);
    }
    return path.toAbsolutePath();
  }

  private static void addFromDefaultsFolder(Map<String, TemplateLocation> sink) {
    Path dir = StreamxHome.getStreamxHome().resolve(DefaultEventTemplates.DIRECTORY);
    if (!Files.isDirectory(dir)) {
      return;
    }
    for (Path file : listJsonFiles(dir)) {
      String id = stripExtension(file, DefaultEventTemplates.EXTENSION);
      sink.put(id, new TemplateLocation(
          id,
          extractCloudEventType(file),
          file.toAbsolutePath().toString(),
          SOURCE_DEFAULT
      ));
    }
  }

  private static void addFromUserFolder(Map<String, TemplateLocation> sink) {
    Path dir = UserEventTemplates.getDirectory();
    if (!Files.isDirectory(dir)) {
      return;
    }
    for (Path file : listJsonFiles(dir)) {
      String id = stripExtension(file, UserEventTemplates.EXTENSION);
      sink.put(id, new TemplateLocation(
          id,
          extractCloudEventType(file),
          file.toAbsolutePath().toString(),
          SOURCE_CUSTOM
      ));
    }
  }

  private static void addFromSettings(Map<String, TemplateLocation> sink) {
    for (Map.Entry<String, Path> entry : readSettingsEntries().entrySet()) {
      String id = entry.getKey();
      Path path = entry.getValue();
      String type = Files.isRegularFile(path) ? extractCloudEventType(path) : null;
      sink.put(id, new TemplateLocation(
          id,
          type,
          path.toString(),
          SOURCE_SETTINGS
      ));
    }
  }

  private static Properties loadConfig() {
    URL url = StreamxHome.getConfigUrl();
    Properties properties = new Properties();
    try (InputStream inputStream = url.openStream()) {
      properties.load(inputStream);
      return properties;
    } catch (IOException e) {
      return null;
    }
  }

  private static List<Path> listJsonFiles(Path dir) {
    List<Path> files = new ArrayList<>();
    try (Stream<Path> stream = Files.list(dir)) {
      stream
          .filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().endsWith(DefaultEventTemplates.EXTENSION))
          .forEach(files::add);
    } catch (IOException e) {
      throw new CliException(
          msg.failedToListEventTemplates(dir.toAbsolutePath().toString(), e.getMessage()), e);
    }
    return files;
  }

  private static String stripExtension(Path file, String extension) {
    String fileName = file.getFileName().toString();
    return fileName.substring(0, fileName.length() - extension.length());
  }

  private static String extractCloudEventType(Path templateFile) {
    try {
      String content = Files.readString(templateFile);
      return MAPPER.readTree(content).path("type").asText(null);
    } catch (Exception e) {
      return null;
    }
  }
}
