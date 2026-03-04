package com.streamx.cli.commands.publish.stream;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.CliException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class SourceValidator {

  public static void validate(String source) {
    URI uri;
    try {
      uri = URI.create(source);
    } catch (IllegalArgumentException e) {
      throw new CliException(msg.invalidSourceUri(source), e);
    }

    if (uri.getScheme() == null) {
      validateFile(source);
    } else if ("file".equalsIgnoreCase(uri.getScheme())) {
      validateFile(uri.getPath());
    }
  }

  private static void validateFile(String path) {
    Path filePath = Path.of(path);
    if (!Files.exists(filePath)) {
      throw new CliException(msg.sourceFileNotFound(path));
    }
    if (!Files.isReadable(filePath)) {
      throw new CliException(msg.sourceFileNotReadable(path));
    }
    if (Files.isDirectory(filePath)) {
      throw new CliException(msg.sourceIsDirectory(path));
    }
  }
}