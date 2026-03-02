package com.streamx.cli.commands.publish.stream;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.CliException;
import java.net.HttpURLConnection;
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
    } else {
      validateRemoteUri(uri);
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

  private static void validateRemoteUri(URI uri) {
    try {
      HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
      connection.setRequestMethod("HEAD");
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);
      int responseCode = connection.getResponseCode();
      if (responseCode == 404) {
        throw new CliException(msg.sourceUriNotFound(uri.toString()));
      } else if (responseCode >= 400) {
        throw new CliException(
            msg.sourceUriNotAccessible(uri.toString(), String.valueOf(responseCode)));
      }
    } catch (CliException e) {
      throw e;
    } catch (Exception e) {
      throw new CliException(msg.sourceUriNotReachable(uri.toString(), e.getMessage()), e);
    }
  }
}