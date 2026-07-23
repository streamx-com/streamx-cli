package com.streamx.cli.commands.settings.eventtemplates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.streamx.cli.commands.publish.event.DefaultEventTemplates;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EventTemplatesTestSupport {

  public static final ObjectMapper JSON = new ObjectMapper();
  public static final YAMLMapper YAML = new YAMLMapper();

  private EventTemplatesTestSupport() {
  }

  /** The default profile's own event templates dir inside the given streamx home. */
  public static Path userTemplatesDir(Path home) {
    return home.resolve("profiles/default/event-templates");
  }

  /** The shared default templates dir inside the given streamx home. */
  public static Path defaultTemplatesDir(Path home) {
    return home.resolve(DefaultEventTemplates.DIRECTORY);
  }

  /** The default profile's application.properties inside the given streamx home. */
  public static Path configFile(Path home) {
    return home.resolve("profiles/default/config/application.properties");
  }

  /**
   * A file inside the default profile's dir - where relative registration paths resolve.
   * Parent directories are created.
   */
  public static Path profileFile(Path home, String name) throws IOException {
    Path file = home.resolve("profiles/default").resolve(name);
    Files.createDirectories(file.getParent());
    return file;
  }

  public static String sampleTemplate(String type) {
    return """
        {
          "specversion": "1.0",
          "id": "${uuid}",
          "source": "test-source",
          "type": "%s",
          "datacontenttype": "application/json",
          "subject": "${subject}",
          "time": "${currentTime}",
          "data": {}
        }
        """.formatted(type);
  }

  public static JsonNode findById(JsonNode templatesArray, String id) {
    for (JsonNode entry : templatesArray) {
      if (id.equals(entry.get("id").asText())) {
        return entry;
      }
    }
    return null;
  }
}
