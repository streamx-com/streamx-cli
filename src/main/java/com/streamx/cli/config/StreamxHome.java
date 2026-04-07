package com.streamx.cli.config;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static com.streamx.cli.util.FileUtils.createIfNotExists;

import com.streamx.cli.commands.publish.event.DefaultEventTemplates;
import com.streamx.cli.framework.CliException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
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

  public static URL getConfigUrl() {
    try {
      return getConfigPath().toUri().toURL();
    } catch (MalformedURLException e) {
      throw new CliException(msg.unableToGetSettingsFilePath(), e);
    }
  }

  public static void createConfigIfNotExists() {
    try {
      Path pathToFile = getConfigPath();
      createIfNotExists(pathToFile.getParent(), pathToFile);
    } catch (IOException e) {
      throw new CliException(msg.unableToGetSettingsFilePath(), e);
    }
  }

  public static void populate() {
    Path templatesDir = getStreamxHome().resolve(DefaultEventTemplates.DIRECTORY);
    if (!Files.exists(templatesDir)) {
      DefaultEventTemplates.populate();
    }

    createConfigIfNotExists();
  }

  static String getStreamxHomeEnv() {
    return System.getenv("STREAMX_HOME");
  }
}