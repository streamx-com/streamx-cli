package com.streamx.cli.commands.completion;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CompletionCommandIT extends CliBaseIT {

  @Test
  void shouldGenerateBashCompletionScript() throws Exception {
    ProcessResult result = exec("completion", "bash");

    result.assertSuccess();
    assertThat(result.stdout()).contains("complete -F");
  }

  @Test
  void shouldGenerateZshCompletionScript() throws Exception {
    ProcessResult result = exec("completion", "zsh");

    result.assertSuccess();
    String stdout = result.stdout();
    assertThat(stdout).contains("#compdef streamx");
    assertThat(stdout).contains("_arguments");
    assertThat(stdout).contains("compdef _streamx streamx");
  }

  /** Option candidates were previously dropped, so --role offered nothing on TAB. */
  @Test
  void shouldEmitRoleCandidatesForZshOptions() throws Exception {
    ProcessResult result = exec("completion", "zsh");

    result.assertSuccess();
    assertThat(result.stdout()).contains("(owner edit view)");
  }

  @Test
  void shouldEmitDynamicTemplateIdCompletionForPublishEvent() throws Exception {
    ProcessResult result = exec("completion", "zsh");
    result.assertSuccess();
    assertThat(result.stdout())
        .contains("$(streamx __complete-template-ids 2>/dev/null)");
  }

  @Test
  void shouldEmitDynamicContextNameCompletion() throws Exception {
    ProcessResult result = exec("completion", "zsh");
    result.assertSuccess();
    assertThat(result.stdout())
        .contains("$(streamx __complete-context-names 2>/dev/null)");
  }

  @Test
  void shouldEmitDynamicOrgAndProjectIdCompletion() throws Exception {
    ProcessResult result = exec("completion", "zsh");
    result.assertSuccess();
    assertThat(result.stdout())
        .contains("$(streamx __complete-org-ids 2>/dev/null)")
        .contains("]:<orgId>:($(streamx __complete-org-ids 2>/dev/null))")
        .contains(
            "$(streamx __complete-project-ids \"${words[${words[(i)--org]}+1]}\" 2>/dev/null)")
        .contains(
            "$(streamx __complete-org-member-ids \"${words[${words[(i)--org]}+1]}\" 2>/dev/null)")
        .contains(
            "$(streamx __complete-invited-emails \"${words[${words[(i)--org]}+1]}\" 2>/dev/null)")
        .contains(
            "$(streamx __complete-cluster-ids \"${words[${words[(i)--org]}+1]}\" 2>/dev/null)");
  }

  @Test
  void shouldHideInternalCompleteTemplateIdsCommandFromZshSubcommands() throws Exception {
    ProcessResult result = exec("completion", "zsh");
    result.assertSuccess();
    assertThat(result.stdout()).doesNotContain("'__complete-template-ids'");
  }

  @Test
  void shouldListAllTemplateIdsViaInternalHelper() throws Exception {
    ProcessResult result = exec("__complete-template-ids");
    result.assertSuccess();
    assertThat(result.stdout()).contains("page.published");
    assertThat(result.stdout()).contains("asset.published");
  }

  @Test
  void shouldCompleteDomainsInBothShells() throws Exception {
    for (String shell : new String[] {"bash", "zsh"}) {
      ProcessResult result = exec("completion", shell);
      result.assertSuccess();
      assertThat(result.stdout()).contains("__complete-domains")
          .contains("_streamx_completion_value --org")
          .contains("_streamx_completion_value --project");
      // Five commands accept an existing domain; add must not suggest existing domains.
      assertThat(result.stdout().split("__complete-domains", -1)).hasSize(6);
    }
  }

  @Test
  void shouldRunScopedBashCompletions() throws Exception {
    ProcessResult generated = exec("completion", "bash");
    generated.assertSuccess();
    Path script = Files.createTempFile("domain-completion-", ".bash");
    try {
      Files.writeString(script, generated.stdout() + """

          compopt() { :; }
          streamx() {
            local IFS=' '
            if [[ "$*" == *"__complete-org-ids" ]]; then
              printf '%s\n' so-alpha so-beta
            elif [[ "$*" == *"__complete-project-ids so-alpha" ]]; then
              printf '%s\n' project-one project-two
            elif [[ "$*" == "--context team __complete-domains so-alpha project-one" ]]; then
              printf '%s\n' shop shop.example.com
            else
              return 1
            fi
          }
          COMP_WORDS=(streamx project domains get --context team --org so-alpha
                      --project project-one sh)
          COMP_CWORD=10
          _picocli_streamx_project_domains_get
          [[ "${COMPREPLY[*]}" == "shop shop.example.com" ]] || exit 1
          COMP_WORDS=(streamx project domains get --org so-)
          COMP_CWORD=5
          _picocli_streamx_project_domains_get
          [[ "${COMPREPLY[*]}" == "so-alpha so-beta" ]] || exit 2
          COMP_WORDS=(streamx project domains get --org so-alpha --project project-)
          COMP_CWORD=7
          _picocli_streamx_project_domains_get
          [[ "${COMPREPLY[*]}" == "project-one project-two" ]] || exit 3
          COMP_WORDS=(streamx project domains get --context=team --org=so-alpha
                      --project=project-one sh)
          COMP_CWORD=7
          _picocli_streamx_project_domains_get
          [[ "${COMPREPLY[*]}" == "shop shop.example.com" ]] || exit 4
          """);
      Process process = new ProcessBuilder("bash", script.toString())
          .redirectErrorStream(true).start();
      assertThat(process.waitFor(10, TimeUnit.SECONDS)).isTrue();
      String output = new String(process.getInputStream().readAllBytes());
      assertThat(process.exitValue()).as(output).isZero();
    } finally {
      Files.deleteIfExists(script);
    }
  }

}
