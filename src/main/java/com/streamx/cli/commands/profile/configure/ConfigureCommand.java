package com.streamx.cli.commands.profile.configure;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.auth.AuthConfig;
import com.streamx.cli.commands.auth.login.LoginCommand;
import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.InteractivePicker;
import com.streamx.cli.framework.InteractivePicker.Session;
import com.streamx.cli.ingestion.IngestionClientConfig;
import com.streamx.cli.platform.PlatformConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import org.eclipse.microprofile.config.ConfigProvider;
import picocli.CommandLine;

@CommandLine.Command(
    name = "configure",
    header = "Interactively configure the active profile",
    description = "Asks for the endpoints the CLI talks to and offers to log in. "
        + "Press Enter to keep the value shown in brackets. Values are written to the "
        + "profile's application.properties (same as `settings set`)."
)
public class ConfigureCommand extends AbstractSilentCommand {

  static final String DEFAULT_AUTH_URL_KEY = "streamx.defaults.auth.server-url";
  static final String DEFAULT_PLATFORM_URL_KEY = "streamx.defaults.platform.url";

  private static final String SKIP = "-";
  private static final String METHOD_BROWSER = "browser";
  private static final String METHOD_DEVICE = "device-code";

  @Override
  public CommandResult<Void> runCommand() {
    Properties settings = loadSettings();

    boolean login;
    boolean device = false;
    try (Session session = InteractivePicker.open()) {
      configureUrl(session, settings, msg.profileConfigurePromptAuthUrl(),
          AuthConfig.STREAMX_AUTH_SERVER_URL, DEFAULT_AUTH_URL_KEY, false);
      configureInsecure(session, settings, AuthConfig.STREAMX_AUTH_INSECURE, "auth");

      configureUrl(session, settings, msg.profileConfigurePromptPlatformUrl(),
          PlatformConfig.STREAMX_PLATFORM_URL, DEFAULT_PLATFORM_URL_KEY, false);
      configureInsecure(session, settings, PlatformConfig.STREAMX_PLATFORM_INSECURE, "platform");

      // No build-time default: ingestion URLs are per-project (see application.properties).
      boolean ingestionSet = configureUrl(session, settings,
          msg.profileConfigurePromptIngestionUrl(),
          IngestionClientConfig.STREAMX_INGESTION_URL, null, true);
      if (ingestionSet) {
        configureInsecure(session, settings,
            IngestionClientConfig.STREAMX_INGESTION_INSECURE, "ingestion");
      }

      storeSettings(settings);
      System.out.println(msg.profileConfigureSaved(StreamxHome.getActiveProfile()));

      login = promptYesNo(session, msg.profileConfigurePromptLogin(), true);
      if (login) {
        String method = session.pick(
            msg.profileConfigurePromptLoginMethod() + " [" + METHOD_BROWSER + "]",
            List.of(METHOD_BROWSER, METHOD_DEVICE));
        if (method != null && !method.isBlank()
            && !METHOD_BROWSER.equalsIgnoreCase(method.strip())
            && !METHOD_DEVICE.equalsIgnoreCase(method.strip())) {
          throw new CliException(msg.profileConfigureInvalidAnswer(method));
        }
        device = method != null && METHOD_DEVICE.equalsIgnoreCase(method.strip());
      }
    }

    if (login) {
      // The wizard's prompt reader is closed; login re-reads config from the file just saved.
      LoginCommand loginCommand = new LoginCommand();
      loginCommand.noBrowser = device;
      return loginCommand.runCommand();
    }
    return new CommandResult<>(null);
  }

  /**
   * Prompts for a URL. Enter keeps the default (current profile value, falling back to the
   * build-time default). For optional URLs, {@code -} skips without touching the settings.
   * Returns whether the key ended up set.
   */
  private boolean configureUrl(Session session, Properties settings, String prompt,
      String settingsKey, String buildTimeDefaultKey, boolean optional) {
    String defaultValue = currentOrBuiltIn(settings, settingsKey, buildTimeDefaultKey);
    String suffix = defaultValue == null ? "" : " [" + defaultValue + "]";
    String answer = session.pick(prompt + suffix, null);

    if (answer != null && SKIP.equals(answer.strip())) {
      if (optional) {
        return settings.getProperty(settingsKey) != null;
      }
      throw new CliException(msg.profileConfigureValueRequired(settingsKey));
    }
    String value = answer == null || answer.isBlank() ? defaultValue : answer.strip();
    if (value == null) {
      if (optional) {
        return false;
      }
      throw new CliException(msg.profileConfigureValueRequired(settingsKey));
    }
    value = value.replaceAll("/+$", "");
    if (!value.matches("https?://.+")) {
      throw new CliException(msg.profileConfigureInvalidUrl(value));
    }
    settings.setProperty(settingsKey, value);
    return true;
  }

  /** Positively phrased so the default "yes" answer yields the secure setting. */
  private void configureInsecure(Session session, Properties settings, String settingsKey,
      String target) {
    boolean currentInsecure = Boolean.parseBoolean(settings.getProperty(settingsKey));
    boolean verify = promptYesNo(
        session, msg.profileConfigurePromptVerifyTls(target), !currentInsecure);
    settings.setProperty(settingsKey, String.valueOf(!verify));
  }

  private boolean promptYesNo(Session session, String prompt, boolean defaultValue) {
    String suffix = defaultValue ? " (Y/n)" : " (y/N)";
    String answer = session.pick(prompt + suffix, null);
    if (answer == null || answer.isBlank()) {
      return defaultValue;
    }
    String normalized = answer.strip().toLowerCase();
    if (normalized.equals("y") || normalized.equals("yes") || normalized.equals("true")) {
      return true;
    }
    if (normalized.equals("n") || normalized.equals("no") || normalized.equals("false")) {
      return false;
    }
    throw new CliException(msg.profileConfigureInvalidAnswer(answer));
  }

  /** Current profile value if present, else the default baked into the CLI at build time. */
  private static String currentOrBuiltIn(Properties settings, String settingsKey,
      String buildTimeDefaultKey) {
    String current = settings.getProperty(settingsKey);
    if (current != null && !current.isBlank()) {
      return current;
    }
    if (buildTimeDefaultKey == null) {
      return null;
    }
    return ConfigProvider.getConfig()
        .getOptionalValue(buildTimeDefaultKey, String.class)
        .orElse(null);
  }

  private static Properties loadSettings() {
    Properties properties = new Properties();
    try (InputStream inputStream = StreamxHome.getConfigUrl().openStream()) {
      properties.load(inputStream);
    } catch (IOException e) {
      throw new CliException(msg.unableToSetSettingsProperty(), e);
    }
    return properties;
  }

  private static void storeSettings(Properties properties) {
    URL url = StreamxHome.getConfigUrl();
    Path path = Paths.get(url.getPath());
    try (OutputStream outputStream = Files.newOutputStream(path)) {
      properties.store(outputStream, null);
    } catch (IOException e) {
      throw new CliException(msg.unableToSetSettingsProperty(), e);
    }
  }
}
