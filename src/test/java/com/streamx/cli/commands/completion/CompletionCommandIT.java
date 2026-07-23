package com.streamx.cli.commands.completion;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
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
  void shouldEmitDynamicProfileNameCompletion() throws Exception {
    ProcessResult result = exec("completion", "zsh");
    result.assertSuccess();
    assertThat(result.stdout())
        .contains("$(streamx __complete-profile-names 2>/dev/null)");
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
}
