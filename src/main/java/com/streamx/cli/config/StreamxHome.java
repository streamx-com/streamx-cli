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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class StreamxHome {
  private static final String DEFAULT_HOME_DIR = ".streamx";
  public static final String DEFAULT_PROFILE = "default";
  private static final String PROFILES_DIR = "profiles";
  private static final String CONFIG_DIR = "config";
  private static final String EVENT_TEMPLATES_DIR = "event-templates";
  private static final String CURRENT_PROFILE_FILE = "current-profile";
  private static final Pattern PROFILE_NAME = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");
  private static String streamxHomeCliArg;
  private static String profileCliArg;
  private static final Set<String> appliedKeys = ConcurrentHashMap.newKeySet();

  public static void setStreamxHomeCliArg(String path) {
    streamxHomeCliArg = path;
  }

  public static void clearStreamxHomeCliArg() {
    streamxHomeCliArg = null;
  }

  public static void setProfileCliArg(String name) {
    profileCliArg = name;
  }

  public static void clearProfileCliArg() {
    profileCliArg = null;
  }

  public static boolean isValidProfileName(String name) {
    return name != null && PROFILE_NAME.matcher(name).matches();
  }

  public static String requireValidProfileName(String name) {
    if (!isValidProfileName(name)) {
      throw new CliException(msg.profileNameInvalid(String.valueOf(name)));
    }
    return name;
  }

  /** Precedence: --profile > STREAMX_PROFILE > current-profile file > default. */
  public static String getActiveProfile() {
    if (profileCliArg != null && !profileCliArg.isBlank()) {
      return requireValidProfileName(profileCliArg.trim());
    }
    String env = System.getProperty("STREAMX_PROFILE");
    if (env == null || env.isBlank()) {
      env = System.getenv("STREAMX_PROFILE");
    }
    if (env != null && !env.isBlank()) {
      return requireValidProfileName(env.trim());
    }
    String stored = readCurrentProfilePointer();
    if (stored != null && !stored.isEmpty()) {
      if (!isValidProfileName(stored)) {
        throw new CliException(
            msg.profileInvalidPointer(stored, getCurrentProfileFile().toString()));
      }
      return stored;
    }
    return DEFAULT_PROFILE;
  }

  /** Trimmed content of the current-profile file, or null if absent or unreadable. */
  public static String readCurrentProfilePointer() {
    Path pointer = getCurrentProfileFile();
    if (!Files.isRegularFile(pointer)) {
      return null;
    }
    try {
      return Files.readString(pointer).trim();
    } catch (IOException expected) {
      return null;
    }
  }

  public static void writeCurrentProfilePointer(String name) throws IOException {
    Path pointer = getCurrentProfileFile();
    Files.createDirectories(pointer.getParent());
    Files.writeString(pointer, name + System.lineSeparator());
  }

  /** Which precedence layer picked the streamx home, for diagnostics ({@code streamx info}). */
  public static String getStreamxHomeSource() {
    if (streamxHomeCliArg != null && !streamxHomeCliArg.isBlank()) {
      return "from the --streamx-home flag";
    }
    String env = getStreamxHomeEnv();
    if (env != null && !env.isBlank()) {
      return "from the STREAMX_HOME environment variable";
    }
    String prop = System.getProperty("STREAMX_HOME");
    if (prop != null && !prop.isBlank()) {
      return "from the STREAMX_HOME system property";
    }
    return "default";
  }

  /** Which precedence layer picked the active profile, for diagnostics ({@code streamx info}). */
  public static String getActiveProfileSource() {
    if (profileCliArg != null && !profileCliArg.isBlank()) {
      return "from the --profile flag";
    }
    String env = System.getProperty("STREAMX_PROFILE");
    if (env == null || env.isBlank()) {
      env = System.getenv("STREAMX_PROFILE");
    }
    if (env != null && !env.isBlank()) {
      return "from the STREAMX_PROFILE environment variable";
    }
    String stored = readCurrentProfilePointer();
    if (stored != null && !stored.isEmpty()) {
      return "from the current-profile file";
    }
    return "default, nothing selected";
  }

  public static Path getCurrentProfileFile() {
    return getStreamxHome().resolve(CURRENT_PROFILE_FILE);
  }

  public static Path getProfilesDir() {
    return getStreamxHome().resolve(PROFILES_DIR);
  }

  public static Path getProfileDirOf(String profile) {
    return getProfilesDir().resolve(profile);
  }

  public static Path getProfileDir() {
    return getProfileDirOf(getActiveProfile());
  }

  public static Path getConfigDirOf(String profile) {
    return getProfileDirOf(profile).resolve(CONFIG_DIR);
  }

  public static Path getConfigDir() {
    return getConfigDirOf(getActiveProfile());
  }

  public static Path getEventTemplatesDirOf(String profile) {
    return getProfileDirOf(profile).resolve(EVENT_TEMPLATES_DIR);
  }

  public static Path getEventTemplatesDir() {
    return getEventTemplatesDirOf(getActiveProfile());
  }

  public static boolean profileExists(String profile) {
    return Files.isDirectory(getProfileDirOf(profile));
  }

  public static List<String> listProfileNames() {
    List<String> names = new ArrayList<>();
    Path profilesDir = getProfilesDir();
    if (Files.isDirectory(profilesDir)) {
      try (Stream<Path> entries = Files.list(profilesDir)) {
        entries.filter(Files::isDirectory)
            .map(path -> path.getFileName().toString())
            .filter(StreamxHome::isValidProfileName)
            .sorted()
            .forEach(names::add);
      } catch (IOException expected) {
      }
    }
    return names;
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
    return getConfigDir().resolve("application.properties");
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

  /**
   * Prepares the active profile for this invocation. The shared default event templates are
   * seeded unconditionally. The default profile is bootstrapped on first use; a missing named
   * profile is an error for commands that operate on profile state ({@code needsProfile}), and
   * is tolerated for profile-management commands so a broken selection can be repaired.
   */
  public static void populate(boolean needsProfile) {

    DefaultEventTemplates.populate();

    String active = getActiveProfile();
    if (!profileExists(active)) {
      if (DEFAULT_PROFILE.equals(active)) {
        bootstrapDefaultProfile();
      } else if (needsProfile) {
        throw new CliException(msg.profileNotFound(active));
      } else {
        clearAppliedSystemProperties();
        return;
      }
    }
    createConfigIfNotExists();
    applySettingsToSystemProperties();
  }

  /** First use: materialize the default profile and, if nothing is selected, point at it. */
  private static void bootstrapDefaultProfile() {
    try {
      Files.createDirectories(getConfigDirOf(DEFAULT_PROFILE));
      Files.createDirectories(getEventTemplatesDirOf(DEFAULT_PROFILE));
      String stored = readCurrentProfilePointer();
      if (stored == null || stored.isEmpty()) {
        writeCurrentProfilePointer(DEFAULT_PROFILE);
      }
    } catch (IOException e) {
      throw new CliException(msg.profileCreateFailed(DEFAULT_PROFILE, e.getMessage()), e);
    }
  }

  /**
   * Loads every key/value from the active profile's {@code application.properties} and forwards
   * them to JVM system properties so any code that reads via {@link
   * org.eclipse.microprofile.config.ConfigProvider} (e.g. {@code StreamxBaseConfig} in
   * the streamx-service-mesh runner) picks them up. System properties already set externally
   * (e.g. via {@code -Dkey=value}) take precedence and are left untouched. Keys applied by
   * a previous call are cleared first so a fresh re-apply reflects the current file.
   */
  public static void applySettingsToSystemProperties() {
    clearAppliedSystemProperties();

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

  private static void clearAppliedSystemProperties() {
    for (String key : appliedKeys) {
      System.clearProperty(key);
    }
    appliedKeys.clear();
  }

  static String getStreamxHomeEnv() {
    return System.getenv("STREAMX_HOME");
  }
}
