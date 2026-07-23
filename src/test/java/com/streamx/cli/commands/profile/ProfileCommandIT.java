package com.streamx.cli.commands.profile;

import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.sampleTemplate;
import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.commands.auth.StubOidcServer;
import com.streamx.cli.commands.org.StubPlatformServer;
import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProfileCommandIT extends CliBaseIT {

  private StubOidcServer oidcServer;

  @BeforeEach
  void cleanProfiles() throws IOException {
    deleteRecursively(streamxHome.resolve("profiles"));
    Files.deleteIfExists(streamxHome.resolve("current-profile"));
  }

  @AfterEach
  void stopServer() {
    if (oidcServer != null) {
      oidcServer.close();
      oidcServer = null;
    }
  }

  @Test
  void firstUseBootstrapsDefaultProfile() throws Exception {
    ProcessResult result = exec("profile", "current");

    result.assertSuccess();
    assertThat(result.stdout().strip()).isEqualTo("default");
    assertThat(streamxHome.resolve("profiles/default/config")).isDirectory();
    assertThat(streamxHome.resolve("profiles/default/event-templates")).isDirectory();
    assertThat(streamxHome.resolve("current-profile")).content().contains("default");
  }

  @Test
  void createSwitchesToNewProfileAndSuggestsConfigure() throws Exception {
    ProcessResult created = exec("profile", "create", "prod");

    created.assertSuccess();
    assertThat(created.stdout())
        .contains("Profile 'prod' created")
        .contains("Switched to profile 'prod'");
    assertThat(created.stderr()).contains("streamx profile configure");
    assertThat(exec("profile", "current").stdout().strip()).isEqualTo("prod");
  }

  @Test
  void createUseCurrentLifecycle() throws Exception {
    exec("profile", "create", "prod").assertSuccess();
    exec("profile", "use", "prod").assertSuccess();

    ProcessResult current = exec("profile", "current");
    current.assertSuccess();
    assertThat(current.stdout().strip()).isEqualTo("prod");

    ProcessResult list = exec("profile", "list");
    list.assertSuccess();
    assertThat(list.stdout()).contains("default").contains("prod").contains("*");

    ProcessResult quiet = exec("profile", "list", "-q");
    quiet.assertSuccess();
    assertThat(quiet.stdout().strip().lines()).containsExactly("default", "prod");
  }

  @Test
  void useMissingProfileFailsHard() throws Exception {
    ProcessResult result = exec("profile", "use", "nope");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains("does not exist");
    assertThat(exec("profile", "current").stdout().strip()).isEqualTo("default");
  }

  @Test
  void corruptedPointerFileErrorsWithPathAndFlagRepairs() throws Exception {
    Files.writeString(streamxHome.resolve("current-profile"), "Bad_Name\n");

    ProcessResult result = exec("profile", "current");
    result.assertExitCode(1);
    assertThat(result.stderr()).contains("Bad_Name").contains("current-profile");

    ProcessResult repaired = exec("profile", "current", "-P", "default");
    repaired.assertSuccess();
    assertThat(repaired.stdout().strip()).isEqualTo("default");
  }

  @Test
  void createRejectsInvalidNamesAndDuplicates() throws Exception {
    assertThat(exec("profile", "create", "Bad_Name").stderr()).contains("Invalid profile name");
    assertThat(exec("profile", "create", "default").stderr()).contains("already exists");

    exec("profile", "create", "dup").assertSuccess();
    assertThat(exec("profile", "create", "dup").stderr()).contains("already exists");
  }

  @Test
  void missingProfileFailsAndCreatesNothing() throws Exception {
    ProcessResult result = exec("settings", "list", "--profile", "ghost");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains("does not exist");
    assertThat(streamxHome.resolve("profiles/ghost")).doesNotExist();

    ProcessResult use = exec("profile", "use", "ghost2", "--profile", "ghost2");
    use.assertExitCode(1);
    assertThat(use.stderr()).contains("does not exist");
    assertThat(streamxHome.resolve("profiles/ghost2")).doesNotExist();
  }

  @Test
  void createFromCopiesSettingsAndTemplatesButNeverCredentials() throws Exception {
    exec("profile", "current").assertSuccess();
    Files.writeString(streamxHome.resolve("profiles/default/config/application.properties"),
        "streamx.platform.url=https://dev.example.com\n");
    Files.writeString(streamxHome.resolve("profiles/default/config/credentials.json"), "{}");
    Files.writeString(streamxHome.resolve("profiles/default/event-templates/mine.json"),
        sampleTemplate("com.example.mine.v1"));

    exec("profile", "create", "clone", "--from", "default").assertSuccess();

    Path cloneDir = streamxHome.resolve("profiles/clone");
    assertThat(cloneDir.resolve("config/application.properties"))
        .content().contains("dev.example.com");
    assertThat(cloneDir.resolve("event-templates/mine.json")).isRegularFile();
    assertThat(cloneDir.resolve("config/credentials.json")).doesNotExist();
  }

  @Test
  void settingsFollowTheActiveProfile() throws Exception {
    exec("profile", "create", "prod").assertSuccess();
    exec("profile", "use", "prod").assertSuccess();

    exec("settings", "set", "streamx.platform.url", "https://prod.example.com").assertSuccess();

    assertThat(streamxHome.resolve("profiles/prod/config/application.properties"))
        .content().contains("prod.example.com");
    Path defaultSettings = streamxHome.resolve("profiles/default/config/application.properties");
    if (Files.exists(defaultSettings)) {
      assertThat(defaultSettings).content().doesNotContain("prod.example.com");
    }
  }

  @Test
  void customTemplatesAndRegistrationsAreProfileScoped() throws Exception {
    exec("profile", "create", "prod").assertSuccess();
    exec("profile", "use", "prod").assertSuccess();
    Files.writeString(streamxHome.resolve("profiles/prod/event-templates/mine.json"),
        sampleTemplate("com.example.mine.v1"));
    Path registeredFile = streamxHome.resolve("reg-src.json");
    Files.writeString(registeredFile, sampleTemplate("com.example.registered.v1"));
    exec("settings", "event-templates", "register", "reg.tpl", registeredFile.toString())
        .assertSuccess();

    ProcessResult prodList = exec("settings", "event-templates", "list");
    prodList.assertSuccess();
    assertThat(prodList.stdout())
        .contains("mine")
        .contains("reg.tpl")
        .contains("page.published");

    exec("profile", "use", "default").assertSuccess();
    ProcessResult defaultList = exec("settings", "event-templates", "list");
    defaultList.assertSuccess();
    assertThat(defaultList.stdout())
        .doesNotContain("mine")
        .doesNotContain("reg.tpl")
        .contains("page.published");
  }

  @Test
  void configureAcceptsBuildTimeDefaultsOnEnter() throws Exception {
    org.eclipse.microprofile.config.Config config =
        org.eclipse.microprofile.config.ConfigProvider.getConfig();
    String authDefault = config.getValue("streamx.defaults.auth.server-url", String.class);
    String platformDefault = config.getValue("streamx.defaults.platform.url", String.class);

    ProcessResult result = execWithStdin("\n\n\n\n\nn\n", "profile", "configure");

    result.assertSuccess();
    assertThat(result.stdout()).contains("Profile 'default' configured");
    Path settings = streamxHome.resolve("profiles/default/config/application.properties");
    assertThat(settings).content()
        .contains("streamx.auth.server-url=" + authDefault.replace(":", "\\:"))
        .contains("streamx.auth.insecure=false")
        .contains("streamx.platform.url=" + platformDefault.replace(":", "\\:"))
        .contains("streamx.platform.insecure=false")
        // Ingestion has no default (per-project URL); Enter leaves it unset.
        .doesNotContain("streamx.ingestion.url");
  }

  @Test
  void configureTakesCustomValuesIncludingIngestion() throws Exception {
    ProcessResult result = execWithStdin(
        "https://kc.example.com/\nn\nhttps://api.example.com\ny\n"
            + "https://in.proj.example.com\nn\nn\n",
        "profile", "configure");

    result.assertSuccess();
    Path settings = streamxHome.resolve("profiles/default/config/application.properties");
    assertThat(settings).content()
        .contains("streamx.auth.server-url=https\\://kc.example.com")
        .contains("streamx.auth.insecure=true")
        .contains("streamx.platform.url=https\\://api.example.com")
        .contains("streamx.platform.insecure=false")
        .contains("streamx.ingestion.url=https\\://in.proj.example.com")
        .contains("streamx.ingestion.insecure=true");
  }

  @Test
  void profileFlagOverridesPointerWithoutChangingIt() throws Exception {
    exec("profile", "create", "prod").assertSuccess();
    exec("profile", "use", "default").assertSuccess();

    ProcessResult overridden = exec("profile", "current", "--profile", "prod");
    overridden.assertSuccess();
    assertThat(overridden.stdout().strip()).isEqualTo("prod");

    assertThat(exec("profile", "current").stdout().strip()).isEqualTo("default");
  }

  @Test
  void globalFlagsWorkAtAnyPosition() throws Exception {
    exec("profile", "create", "prod").assertSuccess();
    // Point the pointer away from prod so each flag position must actually override it.
    exec("profile", "use", "default").assertSuccess();

    ProcessResult root = exec("--profile", "prod", "profile", "current");
    root.assertSuccess();
    assertThat(root.stdout().strip()).isEqualTo("prod");

    ProcessResult mid = exec("profile", "--profile", "prod", "current");
    mid.assertSuccess();
    assertThat(mid.stdout().strip()).isEqualTo("prod");

    ProcessResult shortFlag = exec("-P", "prod", "profile", "current");
    shortFlag.assertSuccess();
    assertThat(shortFlag.stdout().strip()).isEqualTo("prod");

    Path altHome = streamxHome.resolve("alt-home");
    ProcessResult alt = exec("-H", altHome.toString(), "profile", "current");
    alt.assertSuccess();
    assertThat(alt.stdout().strip()).isEqualTo("default");
    assertThat(altHome.resolve("profiles/default/config")).isDirectory();
  }

  @Test
  void loginWritesIntoTheActiveProfile() throws Exception {
    oidcServer = new StubOidcServer("streamx", 0);
    exec("profile", "create", "prod").assertSuccess();
    exec("profile", "use", "prod").assertSuccess();
    Files.writeString(streamxHome.resolve("profiles/prod/config/application.properties"), """
        streamx.auth.server-url=%s
        streamx.auth.realm=streamx
        streamx.auth.client-id=streamx-cli
        """.formatted(oidcServer.getServerUrl()));

    exec("auth", "login", "--no-browser").assertSuccess();

    assertThat(streamxHome.resolve("profiles/prod/config/credentials.json")).exists();
    assertThat(streamxHome.resolve("profiles/default/config/credentials.json")).doesNotExist();
  }

  @Test
  void helpHeaderShowsCurrentProfile() throws Exception {
    ProcessResult defaultHelp = exec("--help");
    defaultHelp.assertSuccess();
    String out = defaultHelp.stdout();
    assertThat(out).contains("Current profile: default");
    assertThat(out).doesNotContain("Usage:");
    assertThat(out.indexOf("Current profile:"))
        .as("profile line renders above the command list")
        .isLessThan(out.indexOf("Commands:"));

    exec("profile", "create", "prod").assertSuccess();
    exec("profile", "use", "prod").assertSuccess();

    ProcessResult prodHelp = exec("--help");
    prodHelp.assertSuccess();
    assertThat(prodHelp.stdout()).contains("Current profile: prod");

    ProcessResult flagHelp = exec("-P", "default", "--help");
    flagHelp.assertSuccess();
    assertThat(flagHelp.stdout())
        .as("help header honors --profile over the pointer")
        .contains("Current profile: default");
  }

  @Test
  void helpHeaderShowsOrgAndProjectContext() throws Exception {
    ProcessResult bareHelp = exec("--help");
    bareHelp.assertSuccess();
    assertThat(bareHelp.stdout())
        .contains("Current profile: default")
        .doesNotContain("Current organization:")
        .doesNotContain("Current project:");

    exec("profile", "org", "use", "acme").assertSuccess();
    exec("profile", "project", "use", "acme-shop").assertSuccess();

    ProcessResult contextHelp = exec("--help");
    contextHelp.assertSuccess();
    String out = contextHelp.stdout();
    assertThat(out)
        .contains("Current organization: acme")
        .contains("Current project: acme-shop");
    assertThat(out.indexOf("Current profile:"))
        .isLessThan(out.indexOf("Current organization:"));
    assertThat(out.indexOf("Current organization:"))
        .isLessThan(out.indexOf("Current project:"));
    assertThat(out.indexOf("Current project:")).isLessThan(out.indexOf("Commands:"));

    setEnv("STREAMX_ORG", "globex");
    try {
      ProcessResult envHelp = exec("--help");
      envHelp.assertSuccess();
      assertThat(envHelp.stdout())
          .as("header reflects the STREAMX_ORG override")
          .contains("Current organization: globex");
    } finally {
      clearEnv("STREAMX_ORG");
    }
  }

  @Test
  void completeProfileNamesListsAllProfiles() throws Exception {
    exec("profile", "create", "prod").assertSuccess();
    exec("profile", "create", "staging").assertSuccess();

    ProcessResult result = exec("__complete-profile-names");

    result.assertSuccess();
    assertThat(result.stdout().strip().lines())
        .containsExactly("default", "prod", "staging");
  }

  @Test
  void deleteRefusesActiveAndCurrentProfile() throws Exception {
    exec("profile", "create", "prod").assertSuccess();
    exec("profile", "use", "prod").assertSuccess();

    assertThat(exec("profile", "delete", "missing").stderr()).contains("does not exist");
    assertThat(exec("profile", "delete", "prod").stderr()).contains("is active");
    assertThat(exec("profile", "delete", "prod", "--profile", "default").stderr())
        .contains("is set as the current profile");
    assertThat(streamxHome.resolve("profiles/prod")).isDirectory();
  }

  @Test
  void deleteDefaultAllowedWhenNotCurrentAndBootstrapRecreatesIt() throws Exception {
    exec("profile", "create", "prod").assertSuccess();
    exec("profile", "use", "prod").assertSuccess();
    Files.writeString(
        streamxHome.resolve("profiles/default/config/credentials.json"), "{}");

    ProcessResult deleted = exec("profile", "delete", "default");
    deleted.assertSuccess();
    assertThat(deleted.stderr()).contains("NOT revoked");
    assertThat(streamxHome.resolve("profiles/default")).doesNotExist();

    assertThat(exec("profile", "use", "default").stderr()).contains("does not exist");

    ProcessResult bootstrapped = exec("profile", "current", "--profile", "default");
    bootstrapped.assertSuccess();
    assertThat(bootstrapped.stdout().strip()).isEqualTo("default");
    assertThat(streamxHome.resolve("profiles/default/config")).isDirectory();
    assertThat(exec("profile", "current").stdout().strip()).isEqualTo("prod");
  }

  private static void deleteRecursively(Path root) throws IOException {
    if (!Files.exists(root)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(root)) {
      paths.sorted(Comparator.reverseOrder()).forEach(path -> {
        try {
          Files.delete(path);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  @Test
  void configureAsksOrgAndProjectAfterLogin() throws Exception {
    oidcServer = new StubOidcServer("streamx", 0);
    try (StubPlatformServer platform = new StubPlatformServer()) {
      String stdin = String.join("\n",
          oidcServer.getServerUrl(),
          "n",
          platform.getUrl(),
          "n",
          "",
          "y",
          "device-code",
          "acme",
          "so-acme-shop-a1b2c") + "\n";

      ProcessResult result = execWithStdin(stdin, "profile", "configure");

      result.assertSuccess();
      assertThat(result.stdout())
          .contains(msg.orgUseSet("acme"))
          .contains(msg.projectUseSet("so-acme-shop-a1b2c"));
      assertThat(streamxHome.resolve("profiles/default/current-org")).content()
          .isEqualToIgnoringNewLines("acme");
      assertThat(streamxHome.resolve("profiles/default/current-project")).content()
          .isEqualToIgnoringNewLines("so-acme-shop-a1b2c");
      assertThat(platform.getRequests())
          .contains("GET /api/v1/organizations", "GET /api/v1/organizations/acme/projects");
    }
  }

  @Test
  void configureSkipsContextWhenPlatformUnreachable() throws Exception {
    oidcServer = new StubOidcServer("streamx", 0);
    String stdin = String.join("\n",
        oidcServer.getServerUrl(),
        "n",
        "https://127.0.0.1:9",        // unreachable platform
        "n",
        "",
        "y",
        "device-code") + "\n";

    ProcessResult result = execWithStdin(stdin, "profile", "configure");

    result.assertSuccess();
    assertThat(result.stderr()).contains("Skipping organization/project selection");
    assertThat(streamxHome.resolve("profiles/default/current-org")).doesNotExist();
  }

  @Test
  void configureRefreshesUrlsBeforeLoginOnReconfigure() throws Exception {
    exec("settings", "set", "streamx.platform.url", "https://127.0.0.1:9").assertSuccess();

    oidcServer = new StubOidcServer("streamx", 0);
    try (StubPlatformServer platform = new StubPlatformServer()) {
      String stdin = String.join("\n",
          oidcServer.getServerUrl(),
          "n",
          platform.getUrl(),
          "n",
          "",
          "y",
          "device-code",
          "acme",
          "") + "\n";                  // skip project

      ProcessResult result = execWithStdin(stdin, "profile", "configure");

      result.assertSuccess();
      assertThat(result.stderr()).doesNotContain("Skipping organization/project selection");
      assertThat(streamxHome.resolve("profiles/default/current-org")).content()
          .isEqualToIgnoringNewLines("acme");
    }
  }

}
