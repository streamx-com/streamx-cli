package com.streamx.cli.commands.completion;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises generated completions by pressing Tab in an isolated Zsh session. */
@QuarkusTest
class ZshCompletionIT extends CliBaseIT {

  @TempDir
  static Path completionDirectory;

  @Test
  void shouldCompleteIdsAndDomainsForEveryExistingDomainArgument() throws Exception {
    for (String command : List.of("get", "remove", "recheck", "get-zone-file")) {
      CompletionResult result = complete("streamx project domains " + command
          + " --org so-alpha --project project-one sh");
      assertThat(result.matches()).containsExactlyInAnyOrder("shop", "shop.example.com");
      assertThat(result.calls()).containsExactly("<__complete-domains><so-alpha><project-one>");
    }
  }

  @Test
  void shouldForwardScopeAndPreserveSpacesInOptionValues() throws Exception {
    for (String options : List.of(
        "--context team --streamx-home '/tmp/team home' --org so-alpha --project project-one",
        "-C team -H '/tmp/team home' --org so-alpha --project project-one",
        "--context=team --streamx-home='/tmp/team home' --org=so-alpha --project=project-one",
        "--context team --streamx-home \"/tmp/team home\" --org so-alpha --project project-one",
        "--context team --streamx-home /tmp/team\\ home --org so-alpha --project project-one")) {
      CompletionResult result = complete("streamx project domains get " + options + " sh");
      assertThat(result.matches()).as("%s%n%s%n%s", options, result.calls(), result.output())
          .containsExactlyInAnyOrder("shop", "shop.example.com");
      assertThat(result.calls()).containsExactly(
          "<--context><team><--streamx-home></tmp/team home>"
              + "<__complete-domains><so-alpha><project-one>");
    }
  }

  @Test
  void shouldPreserveEmptyScopeArgumentsForDefaultSelection() throws Exception {
    CompletionResult defaults = complete("streamx project domains get sh");
    assertThat(defaults.matches()).containsExactlyInAnyOrder("shop", "shop.example.com");
    assertThat(defaults.calls()).containsExactly("<__complete-domains><><>");

    CompletionResult org = complete("streamx project domains get --org so-alpha sh");
    assertThat(org.matches()).containsExactlyInAnyOrder("shop", "shop.example.com");
    assertThat(org.calls()).containsExactly("<__complete-domains><so-alpha><>");

    CompletionResult project = complete("streamx project domains get --project project-one sh");
    assertThat(project.matches()).containsExactlyInAnyOrder("shop", "shop.example.com");
    assertThat(project.calls()).containsExactly("<__complete-domains><><project-one>");
  }

  @Test
  void shouldCompleteOrganizationAndProjectOptions() throws Exception {
    CompletionResult org = complete("streamx project domains get --org so-");
    assertThat(org.matches()).containsExactlyInAnyOrder("so-alpha", "so-beta");
    assertThat(org.calls()).containsExactly("<__complete-org-ids>");

    CompletionResult project = complete(
        "streamx project domains get --org so-alpha --project project-");
    assertThat(project.matches()).containsExactlyInAnyOrder("project-one", "project-two");
    assertThat(project.calls()).containsExactly("<__complete-project-ids><so-alpha>");
  }

  @Test
  void shouldKeepTemplateAndSettingsCompletionsWorking() throws Exception {
    assertCompletion("streamx publish event custom.", "<__complete-template-ids>",
        "custom.page", "custom.asset");
    assertCompletion("streamx settings event-templates copy custom.",
        "<__complete-template-ids>", "custom.page", "custom.asset");
    assertCompletion("streamx settings event-templates rename editable.",
        "<__complete-non-default-template-ids>", "editable.page");
    assertCompletion("streamx settings event-templates unregister registered.",
        "<__complete-registered-template-ids>", "registered.page");
    assertCompletion("streamx settings set custom.", "<__complete-settings-set-keys>",
        "custom.new", "custom.saved");
    assertCompletion("streamx settings get custom.", "<__complete-settings-keys>", "custom.saved");
    assertCompletion("streamx settings unset custom.",
        "<__complete-settings-keys>", "custom.saved");
  }

  @Test
  void shouldNotSuggestExistingDomainsForAddOrList() throws Exception {
    for (String arguments : List.of("add sh", "add shop sh", "list sh")) {
      CompletionResult result = complete("streamx project domains " + arguments);
      assertThat(result.matches()).isEmpty();
      assertThat(result.calls()).isEmpty();
    }
  }

  @Test
  void shouldRemainSilentWhenDomainLookupFails() throws Exception {
    CompletionResult result = complete("streamx project domains get sh", true);
    assertThat(result.matches()).isEmpty();
    assertThat(result.calls()).containsExactly("<__complete-domains><><>");
    assertThat(result.output()).doesNotContain("helper unavailable");
  }

  private void assertCompletion(String command, String call, String... matches) throws Exception {
    CompletionResult result = complete(command);
    assertThat(result.matches()).as(command).containsExactlyInAnyOrder(matches);
    assertThat(result.calls()).as(command).containsExactly(call);
  }

  private CompletionResult complete(String command) throws Exception {
    return complete(command, false);
  }

  private CompletionResult complete(String command, boolean failHelper) throws Exception {
    ProcessResult generated = exec("completion", "zsh");
    generated.assertSuccess();
    Path directory = Files.createTempDirectory(completionDirectory, "session-");
    Path script = directory.resolve("completion.zsh");
    Files.writeString(script, generated.stdout());
    Path harness = directory.resolve("harness.zsh");
    try (InputStream source = getClass().getResourceAsStream("/completion/zsh-completion.zsh")) {
      assertThat(source).isNotNull();
      Files.copy(source, harness);
    }
    Path calls = Files.createFile(directory.resolve("calls.log"));
    Path matches = Files.createFile(directory.resolve("matches.log"));
    Path output = directory.resolve("terminal.log");
    ProcessBuilder builder = new ProcessBuilder("zsh", "-f", harness.toString(), script.toString(),
        calls.toString(), matches.toString(), command, Boolean.toString(failHelper))
        .redirectErrorStream(true).redirectOutput(output.toFile());
    builder.environment().put("ZDOTDIR", directory.toString());
    Process process = builder.start();
    try {
      assertThat(process.waitFor(15, TimeUnit.SECONDS)).as("completion finished: %s", command)
          .isTrue();
      String terminalOutput = Files.readString(output);
      assertThat(process.exitValue()).as("%s%n%s", command, terminalOutput).isZero();
      List<String> lines = Files.readAllLines(matches);
      assertThat(lines).startsWith("READY").endsWith("DONE");
      return new CompletionResult(lines.subList(1, lines.size() - 1),
          Files.readAllLines(calls), terminalOutput);
    } finally {
      if (process.isAlive()) {
        process.destroyForcibly().waitFor();
      }
    }
  }

  private record CompletionResult(List<String> matches, List<String> calls, String output) {
  }
}
