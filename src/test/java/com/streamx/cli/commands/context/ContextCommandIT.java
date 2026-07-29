package com.streamx.cli.commands.context;

import static com.streamx.cli.commands.settings.eventtemplates.EventTemplatesTestSupport.sampleTemplate;
import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.commands.auth.StubOidcServer;
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
class ContextCommandIT extends CliBaseIT {

  private StubOidcServer oidcServer;

  @BeforeEach
  void cleanContexts() throws IOException {
    deleteRecursively(streamxHome.resolve("contexts"));
    Files.deleteIfExists(streamxHome.resolve("current-context"));
  }

  @AfterEach
  void stopServer() {
    if (oidcServer != null) {
      oidcServer.close();
      oidcServer = null;
    }
  }

  @Test
  void firstUseBootstrapsDefaultContext() throws Exception {
    ProcessResult result = exec("context", "current");

    result.assertSuccess();
    assertThat(result.stdout().strip()).isEqualTo("default");
    assertThat(streamxHome.resolve("contexts/default/config")).isDirectory();
    assertThat(streamxHome.resolve("contexts/default/event-templates")).isDirectory();
    assertThat(streamxHome.resolve("current-context")).content().contains("default");
  }

  @Test
  void createSwitchesToNewContextAndSuggestsConfigure() throws Exception {
    ProcessResult created = exec("context", "create", "prod");

    created.assertSuccess();
    assertThat(created.stdout())
        .contains("Context 'prod' created")
        .contains("Switched to context 'prod'");
    assertThat(created.stderr()).contains("streamx context configure");
    assertThat(exec("context", "current").stdout().strip()).isEqualTo("prod");
  }

  @Test
  void createUseCurrentLifecycle() throws Exception {
    exec("context", "create", "prod").assertSuccess();
    exec("context", "use", "prod").assertSuccess();

    ProcessResult current = exec("context", "current");
    current.assertSuccess();
    assertThat(current.stdout().strip()).isEqualTo("prod");

    ProcessResult list = exec("context", "list");
    list.assertSuccess();
    assertThat(list.stdout()).contains("default").contains("prod").contains("*");

    ProcessResult quiet = exec("context", "list", "-q");
    quiet.assertSuccess();
    assertThat(quiet.stdout().strip().lines()).containsExactly("default", "prod");
  }

  @Test
  void useMissingContextFailsHard() throws Exception {
    ProcessResult result = exec("context", "use", "nope");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains("does not exist");
    assertThat(exec("context", "current").stdout().strip()).isEqualTo("default");
  }

  @Test
  void corruptedPointerFileErrorsWithPathAndFlagRepairs() throws Exception {
    Files.writeString(streamxHome.resolve("current-context"), "Bad_Name\n");

    ProcessResult result = exec("context", "current");
    result.assertExitCode(1);
    assertThat(result.stderr()).contains("Bad_Name").contains("current-context");

    ProcessResult repaired = exec("context", "current", "-C", "default");
    repaired.assertSuccess();
    assertThat(repaired.stdout().strip()).isEqualTo("default");
  }

  @Test
  void createRejectsInvalidNamesAndDuplicates() throws Exception {
    assertThat(exec("context", "create", "Bad_Name").stderr()).contains("Invalid context name");
    assertThat(exec("context", "create", "default").stderr()).contains("already exists");

    exec("context", "create", "dup").assertSuccess();
    assertThat(exec("context", "create", "dup").stderr()).contains("already exists");
  }

  @Test
  void missingContextFailsAndCreatesNothing() throws Exception {
    ProcessResult result = exec("settings", "list", "--context", "ghost");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains("does not exist");
    assertThat(streamxHome.resolve("contexts/ghost")).doesNotExist();

    ProcessResult use = exec("context", "use", "ghost2", "--context", "ghost2");
    use.assertExitCode(1);
    assertThat(use.stderr()).contains("does not exist");
    assertThat(streamxHome.resolve("contexts/ghost2")).doesNotExist();
  }

  @Test
  void createFromCopiesSettingsAndTemplatesButNeverCredentials() throws Exception {
    exec("context", "current").assertSuccess();
    Files.writeString(streamxHome.resolve("contexts/default/config/application.properties"),
        "streamx.platform.url=https://dev.example.com\n");
    Files.writeString(streamxHome.resolve("contexts/default/config/credentials.json"), "{}");
    Files.writeString(streamxHome.resolve("contexts/default/event-templates/mine.json"),
        sampleTemplate("com.example.mine.v1"));

    exec("context", "create", "clone", "--from", "default").assertSuccess();

    Path cloneDir = streamxHome.resolve("contexts/clone");
    assertThat(cloneDir.resolve("config/application.properties"))
        .content().contains("dev.example.com");
    assertThat(cloneDir.resolve("event-templates/mine.json")).isRegularFile();
    assertThat(cloneDir.resolve("config/credentials.json")).doesNotExist();
  }

  @Test
  void settingsFollowTheActiveContext() throws Exception {
    exec("context", "create", "prod").assertSuccess();
    exec("context", "use", "prod").assertSuccess();

    exec("settings", "set", "streamx.platform.url", "https://prod.example.com").assertSuccess();

    assertThat(streamxHome.resolve("contexts/prod/config/application.properties"))
        .content().contains("prod.example.com");
    Path defaultSettings = streamxHome.resolve("contexts/default/config/application.properties");
    if (Files.exists(defaultSettings)) {
      assertThat(defaultSettings).content().doesNotContain("prod.example.com");
    }
  }

  @Test
  void customTemplatesAndRegistrationsAreContextScoped() throws Exception {
    exec("context", "create", "prod").assertSuccess();
    exec("context", "use", "prod").assertSuccess();
    Files.writeString(streamxHome.resolve("contexts/prod/event-templates/mine.json"),
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

    exec("context", "use", "default").assertSuccess();
    ProcessResult defaultList = exec("settings", "event-templates", "list");
    defaultList.assertSuccess();
    assertThat(defaultList.stdout())
        .doesNotContain("mine")
        .doesNotContain("reg.tpl")
        .contains("page.published");
  }

  @Test
  void contextFlagOverridesPointerWithoutChangingIt() throws Exception {
    exec("context", "create", "prod").assertSuccess();
    exec("context", "use", "default").assertSuccess();

    ProcessResult overridden = exec("context", "current", "--context", "prod");
    overridden.assertSuccess();
    assertThat(overridden.stdout().strip()).isEqualTo("prod");

    assertThat(exec("context", "current").stdout().strip()).isEqualTo("default");
  }

  @Test
  void globalFlagsWorkAtAnyPosition() throws Exception {
    exec("context", "create", "prod").assertSuccess();
    // Point the pointer away from prod so each flag position must actually override it.
    exec("context", "use", "default").assertSuccess();

    ProcessResult root = exec("--context", "prod", "context", "current");
    root.assertSuccess();
    assertThat(root.stdout().strip()).isEqualTo("prod");

    ProcessResult mid = exec("context", "--context", "prod", "current");
    mid.assertSuccess();
    assertThat(mid.stdout().strip()).isEqualTo("prod");

    ProcessResult shortFlag = exec("-C", "prod", "context", "current");
    shortFlag.assertSuccess();
    assertThat(shortFlag.stdout().strip()).isEqualTo("prod");

    Path altHome = streamxHome.resolve("alt-home");
    ProcessResult alt = exec("-H", altHome.toString(), "context", "current");
    alt.assertSuccess();
    assertThat(alt.stdout().strip()).isEqualTo("default");
    assertThat(altHome.resolve("contexts/default/config")).isDirectory();
  }

  @Test
  void loginWritesIntoTheActiveContext() throws Exception {
    oidcServer = new StubOidcServer("streamx", 0);
    exec("context", "create", "prod").assertSuccess();
    exec("context", "use", "prod").assertSuccess();
    Files.writeString(streamxHome.resolve("contexts/prod/config/application.properties"), """
        streamx.auth.server-url=%s
        streamx.auth.realm=streamx
        streamx.auth.client-id=streamx-cli
        """.formatted(oidcServer.getServerUrl()));

    exec("auth", "login", "--no-browser").assertSuccess();

    assertThat(streamxHome.resolve("contexts/prod/config/credentials.json")).exists();
    assertThat(streamxHome.resolve("contexts/default/config/credentials.json")).doesNotExist();
  }

  @Test
  void helpHeaderShowsCurrentContext() throws Exception {
    ProcessResult defaultHelp = exec("--help");
    defaultHelp.assertSuccess();
    String out = defaultHelp.stdout();
    assertThat(out).contains("Current context: default");
    assertThat(out).doesNotContain("Usage:");
    assertThat(out.indexOf("Current context:"))
        .as("context line renders above the command list")
        .isLessThan(out.indexOf("Commands:"));

    exec("context", "create", "prod").assertSuccess();
    exec("context", "use", "prod").assertSuccess();

    ProcessResult prodHelp = exec("--help");
    prodHelp.assertSuccess();
    assertThat(prodHelp.stdout()).contains("Current context: prod");

    ProcessResult flagHelp = exec("-C", "default", "--help");
    flagHelp.assertSuccess();
    assertThat(flagHelp.stdout())
        .as("help header honors --context over the pointer")
        .contains("Current context: default");
  }

  @Test
  void completeContextNamesListsAllContexts() throws Exception {
    exec("context", "create", "prod").assertSuccess();
    exec("context", "create", "staging").assertSuccess();

    ProcessResult result = exec("__complete-context-names");

    result.assertSuccess();
    assertThat(result.stdout().strip().lines())
        .containsExactly("default", "prod", "staging");
  }

  @Test
  void deleteRefusesActiveAndCurrentContext() throws Exception {
    exec("context", "create", "prod").assertSuccess();
    exec("context", "use", "prod").assertSuccess();

    assertThat(exec("context", "delete", "missing").stderr()).contains("does not exist");
    assertThat(exec("context", "delete", "prod").stderr()).contains("is active");
    assertThat(exec("context", "delete", "prod", "--context", "default").stderr())
        .contains("is set as the current context");
    assertThat(streamxHome.resolve("contexts/prod")).isDirectory();
  }

  @Test
  void deleteDefaultAllowedWhenNotCurrentAndBootstrapRecreatesIt() throws Exception {
    exec("context", "create", "prod").assertSuccess();
    exec("context", "use", "prod").assertSuccess();
    Files.writeString(
        streamxHome.resolve("contexts/default/config/credentials.json"), "{}");

    ProcessResult deleted = exec("context", "delete", "default");
    deleted.assertSuccess();
    assertThat(deleted.stderr()).contains("NOT revoked");
    assertThat(streamxHome.resolve("contexts/default")).doesNotExist();

    assertThat(exec("context", "use", "default").stderr()).contains("does not exist");

    ProcessResult bootstrapped = exec("context", "current", "--context", "default");
    bootstrapped.assertSuccess();
    assertThat(bootstrapped.stdout().strip()).isEqualTo("default");
    assertThat(streamxHome.resolve("contexts/default/config")).isDirectory();
    assertThat(exec("context", "current").stdout().strip()).isEqualTo("prod");
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

}
