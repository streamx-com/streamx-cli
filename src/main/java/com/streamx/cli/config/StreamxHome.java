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
  public static final String DEFAULT_CONTEXT = "default";
  private static final String CONTEXTS_DIR = "contexts";
  private static final String CONFIG_DIR = "config";
  private static final String EVENT_TEMPLATES_DIR = "event-templates";
  private static final String CURRENT_CONTEXT_FILE = "current-context";
  private static final Pattern CONTEXT_NAME = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");
  private static String streamxHomeCliArg;
  private static String contextCliArg;
  private static final Set<String> appliedKeys = ConcurrentHashMap.newKeySet();

  public static void setStreamxHomeCliArg(String path) {
    streamxHomeCliArg = path;
  }

  public static void clearStreamxHomeCliArg() {
    streamxHomeCliArg = null;
  }

  public static void setContextCliArg(String name) {
    contextCliArg = name;
  }

  public static void clearContextCliArg() {
    contextCliArg = null;
  }

  public static boolean isValidContextName(String name) {
    return name != null && CONTEXT_NAME.matcher(name).matches();
  }

  public static String requireValidContextName(String name) {
    if (!isValidContextName(name)) {
      throw new CliException(msg.contextNameInvalid(String.valueOf(name)));
    }
    return name;
  }

  /** Precedence: --context > STREAMX_CONTEXT > current-context file > default. */
  public static String getActiveContext() {
    if (contextCliArg != null && !contextCliArg.isBlank()) {
      return requireValidContextName(contextCliArg.trim());
    }
    String env = System.getProperty("STREAMX_CONTEXT");
    if (env == null || env.isBlank()) {
      env = System.getenv("STREAMX_CONTEXT");
    }
    if (env != null && !env.isBlank()) {
      return requireValidContextName(env.trim());
    }
    String stored = readCurrentContextPointer();
    if (stored != null && !stored.isEmpty()) {
      if (!isValidContextName(stored)) {
        throw new CliException(
            msg.contextInvalidPointer(stored, getCurrentContextFile().toString()));
      }
      return stored;
    }
    return DEFAULT_CONTEXT;
  }

  /** Trimmed content of the current-context file, or null if absent or unreadable. */
  public static String readCurrentContextPointer() {
    Path pointer = getCurrentContextFile();
    if (!Files.isRegularFile(pointer)) {
      return null;
    }
    try {
      return Files.readString(pointer).trim();
    } catch (IOException expected) {
      return null;
    }
  }

  public static void writeCurrentContextPointer(String name) throws IOException {
    Path pointer = getCurrentContextFile();
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

  /** Which precedence layer picked the active context, for diagnostics ({@code streamx info}). */
  public static String getActiveContextSource() {
    if (contextCliArg != null && !contextCliArg.isBlank()) {
      return "from the --context flag";
    }
    String env = System.getProperty("STREAMX_CONTEXT");
    if (env == null || env.isBlank()) {
      env = System.getenv("STREAMX_CONTEXT");
    }
    if (env != null && !env.isBlank()) {
      return "from the STREAMX_CONTEXT environment variable";
    }
    String stored = readCurrentContextPointer();
    if (stored != null && !stored.isEmpty()) {
      return "from the current-context file";
    }
    return "default, nothing selected";
  }

  public static Path getCurrentContextFile() {
    return getStreamxHome().resolve(CURRENT_CONTEXT_FILE);
  }

  public static Path getCurrentOrgFile() {
    return getContextDir().resolve("current-org");
  }

  public static Path getCurrentProjectFile() {
    return getContextDir().resolve("current-project");
  }

  public static String readCurrentOrg() {
    return readPointerFile(getCurrentOrgFile());
  }

  public static String readCurrentProject() {
    return readPointerFile(getCurrentProjectFile());
  }

  public static void writeCurrentOrg(String orgId) throws IOException {
    writePointerFile(getCurrentOrgFile(), orgId);
  }

  public static void writeCurrentProject(String projectId) throws IOException {
    writePointerFile(getCurrentProjectFile(), projectId);
  }

  public static void clearCurrentOrg() throws IOException {
    Files.deleteIfExists(getCurrentOrgFile());
  }

  public static void clearCurrentProject() throws IOException {
    Files.deleteIfExists(getCurrentProjectFile());
  }

  private static String readPointerFile(Path pointer) {
    if (!Files.isRegularFile(pointer)) {
      return null;
    }
    try {
      String stored = Files.readString(pointer).trim();
      return stored.isEmpty() ? null : stored;
    } catch (IOException expected) {
      return null;
    }
  }

  private static void writePointerFile(Path pointer, String value) throws IOException {
    Files.createDirectories(pointer.getParent());
    Files.writeString(pointer, value + System.lineSeparator());
  }

  public static Path getContextsDir() {
    return getStreamxHome().resolve(CONTEXTS_DIR);
  }

  public static Path getContextDirOf(String context) {
    return getContextsDir().resolve(context);
  }

  public static Path getContextDir() {
    return getContextDirOf(getActiveContext());
  }

  public static Path getConfigDirOf(String context) {
    return getContextDirOf(context).resolve(CONFIG_DIR);
  }

  public static Path getConfigDir() {
    return getConfigDirOf(getActiveContext());
  }

  public static Path getEventTemplatesDirOf(String context) {
    return getContextDirOf(context).resolve(EVENT_TEMPLATES_DIR);
  }

  public static Path getEventTemplatesDir() {
    return getEventTemplatesDirOf(getActiveContext());
  }

  public static boolean contextExists(String context) {
    return Files.isDirectory(getContextDirOf(context));
  }

  public static List<String> listContextNames() {
    List<String> names = new ArrayList<>();
    Path contextsDir = getContextsDir();
    if (Files.isDirectory(contextsDir)) {
      try (Stream<Path> entries = Files.list(contextsDir)) {
        entries.filter(Files::isDirectory)
            .map(path -> path.getFileName().toString())
            .filter(StreamxHome::isValidContextName)
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

  public static void populate(boolean needsContext) {

    DefaultEventTemplates.populate();

    String active = getActiveContext();
    if (!contextExists(active)) {
      if (DEFAULT_CONTEXT.equals(active)) {
        bootstrapDefaultContext();
      } else if (needsContext) {
        throw new CliException(msg.contextNotFound(active));
      } else {
        clearAppliedSystemProperties();
        return;
      }
    }
    createConfigIfNotExists();
    applySettingsToSystemProperties();
  }

  private static void bootstrapDefaultContext() {
    try {
      Files.createDirectories(getConfigDirOf(DEFAULT_CONTEXT));
      Files.createDirectories(getEventTemplatesDirOf(DEFAULT_CONTEXT));
      String stored = readCurrentContextPointer();
      if (stored == null || stored.isEmpty()) {
        writeCurrentContextPointer(DEFAULT_CONTEXT);
      }
    } catch (IOException e) {
      throw new CliException(msg.contextCreateFailed(DEFAULT_CONTEXT, e.getMessage()), e);
    }
  }

  /**
   * Loads every key/value from the active context's {@code application.properties} and forwards
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
