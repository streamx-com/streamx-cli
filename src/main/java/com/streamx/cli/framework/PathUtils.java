package com.streamx.cli.framework;

import static com.streamx.cli.i18n.MessageProvider.msg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public final class PathUtils {

  private PathUtils() {
  }

  public static void deleteRecursivelyIfExists(Path dir) {
    if (!Files.exists(dir)) {
      return;
    }
    try (Stream<Path> stream = Files.walk(dir)) {
      stream
          .sorted(Comparator.reverseOrder())
          .forEach(p -> {
            try {
              Files.deleteIfExists(p);
            } catch (IOException e) {
              throw new CliException(
                  msg.pathDeleteFailed(p.toAbsolutePath().toString(), e.getMessage()), e);
            }
          });
    } catch (IOException e) {
      throw new CliException(
          msg.pathDeleteFailed(dir.toAbsolutePath().toString(), e.getMessage()), e);
    }
  }
}
