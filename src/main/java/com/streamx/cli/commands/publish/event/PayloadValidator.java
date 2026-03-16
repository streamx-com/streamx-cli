package com.streamx.cli.commands.publish.event;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.CliException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class PayloadValidator {

  public static void validate(String payloadPath) {
    URI uri;
    try {
      uri = URI.create(payloadPath);
    } catch (IllegalArgumentException e) {
      throw new CliException(msg.invalidPayloadPath(payloadPath), e);
    }

    if (uri.getScheme() == null) {
      validateFile(payloadPath);
    } else if ("file".equalsIgnoreCase(uri.getScheme())) {
      validateFile(uri.getPath());
    }
  }

  private static void validateFile(String path) {
    Path filePath = Path.of(path);
    if (!Files.exists(filePath)) {
      throw new CliException(msg.payloadFileNotFound(path));
    }
    if (!Files.isReadable(filePath)) {
      throw new CliException(msg.payloadFileNotReadable(path));
    }
    if (Files.isDirectory(filePath)) {
      throw new CliException(msg.payloadFileIsDirectory(path));
    }
  }
}