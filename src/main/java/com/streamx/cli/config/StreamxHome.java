package com.streamx.cli.config;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static com.streamx.cli.util.FileUtils.createIfNotExists;

import com.streamx.cli.framework.CliException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

public class StreamxHome {
  private static final String DEFAULT_HOME_DIR = ".streamx";
  private static String streamxHomeCliArg;

  public static void setStreamxHomeCliArg(String path) {
    streamxHomeCliArg = path;
  }

  public static void clearStreamxHomeCliArg() {
    streamxHomeCliArg = null;
  }

  public static Path getStreamxHome() {
    if (streamxHomeCliArg != null && !streamxHomeCliArg.isBlank()) {
      return Path.of(streamxHomeCliArg);
    }
    String streamxHome = getStreamxHomeEnv();
    if (streamxHome == null || streamxHome.isBlank()) {
      streamxHome = System.getProperty("STREAMX_HOME");
    }
    if (streamxHome != null && !streamxHome.isBlank()) {
      return Path.of(streamxHome);
    }
    String homeDir = System.getProperty("user.home");
    return Path.of(homeDir, DEFAULT_HOME_DIR);
  }

  public static Path getConfigPath() {
    return getStreamxHome().resolve("config/application.properties");
  }

  public static URL getConfigUrl() throws CliException {
    try {
      Path pathToFile = getConfigPath();
      File file = createIfNotExists(pathToFile.getParent(), pathToFile);

      return file.toURI().toURL();
    } catch (IOException e) {
      throw new CliException(msg.unableToGetSettingsFilePath(), e);
    }
  }

  static String getStreamxHomeEnv() {
    return System.getenv("STREAMX_HOME");
  }
}