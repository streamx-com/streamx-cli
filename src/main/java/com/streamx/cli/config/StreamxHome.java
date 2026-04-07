package com.streamx.cli.config;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static com.streamx.cli.util.FileUtils.createIfNotExists;

import com.streamx.cli.framework.CliException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StreamxHome {
  private static final String DEFAULT_HOME_DIR = ".streamx";
  private static String streamxHomeCliArg;
  private static final Set<String> appliedKeys = ConcurrentHashMap.newKeySet();

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

  public static void applySettingsToSystemProperties() {
    for (String key : appliedKeys) {
      System.clearProperty(key);
    }
    appliedKeys.clear();

    Path configPath = getConfigPath();
    if (!Files.isRegularFile(configPath)) {
      return;
    }
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(configPath)) {
      props.load(in);
    } catch (IOException e) {
      return;
    }
    for (String key : props.stringPropertyNames()) {
      if (System.getProperty(key) != null) {
        continue;
      }
      System.setProperty(key, props.getProperty(key));
      appliedKeys.add(key);
    }
  }
}