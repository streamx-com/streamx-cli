package com.streamx.cli.config;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.CliException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Bundles settings, event templates and login per environment.
 * The behavior is similar to context-related `kubectl config` subcommands.
 *  */
public final class Contexts {

  public static final String DEFAULT_CONTEXT = "default";
  private static final String CONTEXTS_DIR = "contexts";
  private static final String CONFIG_DIR = "config";
  private static final String EVENT_TEMPLATES_DIR = "event-templates";
  private static final String CURRENT_CONTEXT_FILE = "current-context";
  private static final Pattern CONTEXT_NAME = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");
  private static String contextCliArg;

  private Contexts() {
  }

  public static void setContextCliArg(String name) {
    contextCliArg = name;
  }

  public static void clearContextCliArg() {
    contextCliArg = null;
  }

  static boolean isValidContextName(String name) {
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

  public static void clearCurrentContextPointer() throws IOException {
    Files.deleteIfExists(getCurrentContextFile());
  }

  private static Path getCurrentContextFile() {
    return StreamxHome.getStreamxHome().resolve(CURRENT_CONTEXT_FILE);
  }

  private static Path getContextsDir() {
    return StreamxHome.getStreamxHome().resolve(CONTEXTS_DIR);
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
            .filter(Contexts::isValidContextName)
            .sorted()
            .forEach(names::add);
      } catch (IOException expected) {
      }
    }
    return names;
  }

  /** Creates the default context's directories and points current-context at it if unset. */
  static void bootstrapDefault() {
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
}
