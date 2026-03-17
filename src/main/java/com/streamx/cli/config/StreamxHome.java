package com.streamx.cli.config;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static com.streamx.cli.util.FileUtils.createIfNotExists;

import com.streamx.cli.framework.CliException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

public class StreamxHome {
  private static final String DEFAULT_CONFIG_DIR = ".streamx/config";

  public static Path getStreamxHome() {
    String streamxHome = getStreamxHomeEnv();
    if (streamxHome == null || streamxHome.isBlank()) {
      streamxHome = System.getProperty("STREAMX_HOME");
    }
    if (streamxHome != null && !streamxHome.isBlank()) {
      return Path.of(streamxHome);
    }
    String homeDir = System.getProperty("user.home");
    return Path.of(homeDir, DEFAULT_CONFIG_DIR);
  }

  public static URL getConfigUrl() throws CliException {
    try {
      Path pathToDir = getStreamxHome();
      Path pathToFile = pathToDir.resolve("application.properties");
      File file = createIfNotExists(pathToDir, pathToFile);

      return file.toURI().toURL();
    } catch (IOException e) {
      throw new CliException(msg.unableToGetSettingsFilePath(), e);
    }
  }

  static String getStreamxHomeEnv() {
    return System.getenv("STREAMX_HOME");
  }
}