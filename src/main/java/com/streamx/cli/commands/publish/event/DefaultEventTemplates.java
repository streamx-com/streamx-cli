package com.streamx.cli.commands.publish.event;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.CliException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DefaultEventTemplates {

  public static final String DIRECTORY = "default-event-templates";
  public static final String EXTENSION = ".json";

  private static final String INDEX_RESOURCE = "/" + DIRECTORY + "/_index";

  private DefaultEventTemplates() {
  }

  public static List<String> templateNames() {
    try (InputStream inputStream =
        DefaultEventTemplates.class.getResourceAsStream(INDEX_RESOURCE)) {
      if (inputStream == null) {
        throw new CliException(msg.defaultEventTemplatesIndexNotFound(INDEX_RESOURCE));
      }
      String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      return content.lines().toList();
    } catch (IOException e) {
      throw new CliException(msg.unableToReadDefaultEventTemplatesIndex(INDEX_RESOURCE), e);
    }
  }

  public static void populate() {
    try {
      Path targetDir = StreamxHome.getStreamxHome().resolve(DIRECTORY);
      Files.createDirectories(targetDir);
      for (String templateName : templateNames()) {
        String content = loadEmbedded(templateName);
        if (content == null) {
          continue;
        }
        Path file = targetDir.resolve(templateName + EXTENSION);
        Files.writeString(file, content, StandardCharsets.UTF_8);
      }
    } catch (IOException ignored) {
      // best-effort
    }
  }

  public static Path resolveOnDisk(String templateName) {
    return StreamxHome.getStreamxHome()
        .resolve(DIRECTORY)
        .resolve(templateName + EXTENSION);
  }

  public static String loadEmbedded(String templateName) {
    String resourcePath = "/" + DIRECTORY + "/" + templateName + EXTENSION;
    try (InputStream inputStream = DefaultEventTemplates.class.getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        return null;
      }
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      return null;
    }
  }
}
