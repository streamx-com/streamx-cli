package com.streamx.cli.commands.context.configure;

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
import com.streamx.cli.platform.OrganizationsApi;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformConfig;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectsApi;
import com.streamx.cli.platform.generated.model.Organization;
import com.streamx.cli.platform.generated.model.Project;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import org.eclipse.microprofile.config.ConfigProvider;
import picocli.CommandLine;

@CommandLine.Command(
    name = "configure",
    header = "Interactively configure the active context",
    description = "Asks for the endpoints the CLI talks to and offers to log in. "
        + "Press Enter to keep the value shown in brackets. Values are written to the "
        + "context's application.properties (same as `settings set`)."
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
      configureUrl(session, settings, msg.contextConfigurePromptAuthUrl(),
          AuthConfig.STREAMX_AUTH_SERVER_URL, DEFAULT_AUTH_URL_KEY, false);
      configureInsecure(session, settings, AuthConfig.STREAMX_AUTH_INSECURE, "auth");

      configureUrl(session, settings, msg.contextConfigurePromptPlatformUrl(),
          PlatformConfig.STREAMX_PLATFORM_URL, DEFAULT_PLATFORM_URL_KEY, false);
      configureInsecure(session, settings, PlatformConfig.STREAMX_PLATFORM_INSECURE, "platform");

      boolean ingestionSet = configureUrl(session, settings,
          msg.contextConfigurePromptIngestionUrl(),
          IngestionClientConfig.STREAMX_INGESTION_URL, null, true);
      if (ingestionSet) {
        configureInsecure(session, settings,
            IngestionClientConfig.STREAMX_INGESTION_INSECURE, "ingestion");
      }

      storeSettings(settings);
      StreamxHome.applySettingsToSystemProperties();
      System.out.println(msg.contextConfigureSaved(StreamxHome.getActiveContext()));

      login = promptYesNo(session, msg.contextConfigurePromptLogin(), true);
      if (login) {
        String method = session.pick(
            msg.contextConfigurePromptLoginMethod() + " [" + METHOD_BROWSER + "]",
            List.of(METHOD_BROWSER, METHOD_DEVICE));
        if (method != null && !method.isBlank()
            && !METHOD_BROWSER.equalsIgnoreCase(method.strip())
            && !METHOD_DEVICE.equalsIgnoreCase(method.strip())) {
          throw new CliException(msg.contextConfigureInvalidAnswer(method));
        }
        device = method != null && METHOD_DEVICE.equalsIgnoreCase(method.strip());

        LoginCommand loginCommand = new LoginCommand();
        loginCommand.noBrowser = device;
        loginCommand.runCommand();

        askOrgAndProject(session);
      }
    }
    return new CommandResult<>(null);
  }

  private void askOrgAndProject(Session session) {
    try (PlatformClients client = PlatformClients.fromConfig()) {
      List<String> orgIds = new OrganizationsApi(client).list().stream()
          .map(Organization::getId)
          .filter(Objects::nonNull)
          .toList();

      String org = session.pick(msg.contextConfigurePromptOrg(), orgIds);
      if (org == null || org.isBlank()) {
        return;
      }
      org = org.strip();
      String clearedProject = PlatformContext.setCurrentOrg(org);
      if (clearedProject != null) {
        System.err.println(msg.orgUseClearedProject(clearedProject));
      }
      System.out.println(msg.orgUseSet(org));

      List<String> projectIds = new ProjectsApi(client).list(org).stream()
          .map(Project::getId)
          .filter(Objects::nonNull)
          .toList();

      String project = session.pick(msg.contextConfigurePromptProject(), projectIds);
      if (project == null || project.isBlank()) {
        return;
      }
      PlatformContext.setCurrentProject(project.strip());
      System.out.println(msg.projectUseSet(project.strip()));
    } catch (RuntimeException fetchFailed) {
      System.err.println(
          msg.contextConfigureContextSkipped(String.valueOf(fetchFailed.getMessage())));
    }
  }

  private boolean configureUrl(Session session, Properties settings, String prompt,
      String settingsKey, String buildTimeDefaultKey, boolean optional) {
    String defaultValue = currentOrBuiltIn(settings, settingsKey, buildTimeDefaultKey);
    String suffix = defaultValue == null ? "" : " [" + defaultValue + "]";
    String answer = session.pick(prompt + suffix, null);

    if (answer != null && SKIP.equals(answer.strip())) {
      if (optional) {
        return settings.getProperty(settingsKey) != null;
      }
      throw new CliException(msg.contextConfigureValueRequired(settingsKey));
    }
    String value = answer == null || answer.isBlank() ? defaultValue : answer.strip();
    if (value == null) {
      if (optional) {
        return false;
      }
      throw new CliException(msg.contextConfigureValueRequired(settingsKey));
    }
    value = value.replaceAll("/+$", "");
    if (!value.matches("https?://.+")) {
      throw new CliException(msg.contextConfigureInvalidUrl(value));
    }
    settings.setProperty(settingsKey, value);
    return true;
  }

  private void configureInsecure(Session session, Properties settings, String settingsKey,
      String target) {
    boolean currentInsecure = Boolean.parseBoolean(settings.getProperty(settingsKey));
    boolean verify = promptYesNo(
        session, msg.contextConfigurePromptVerifyTls(target), !currentInsecure);
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
    throw new CliException(msg.contextConfigureInvalidAnswer(answer));
  }

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
