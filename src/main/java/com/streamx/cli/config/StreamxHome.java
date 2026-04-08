package com.streamx.cli.config;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static com.streamx.cli.util.FileUtils.createIfNotExists;

import com.streamx.cli.commands.publish.event.DefaultEventTemplates;
import com.streamx.cli.framework.CliException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
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

    DefaultEventTemplates.populate();
    createConfigIfNotExists();
    applySettingsToSystemProperties();
  }

  /**
   * Loads every key/value from {@code streamxHome/config/application.properties} and forwards
   * them to JVM system properties so any code that reads via {@link
   * org.eclipse.microprofile.config.ConfigProvider} (e.g. {@code StreamxBaseConfig} in
   * the streamx-service-mesh runner) picks them up. System properties already set externally
   * (e.g. via {@code -Dkey=value}) take precedence and are left untouched. Keys applied by
   * a previous call are cleared first so a fresh re-apply reflects the current file.
   */
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
    } catch (IOException ignored) {
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

  static String getStreamxHomeEnv() {
    return System.getenv("STREAMX_HOME");
  }
}
