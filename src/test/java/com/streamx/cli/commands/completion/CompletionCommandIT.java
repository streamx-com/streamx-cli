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
    assertThat(stdout).contains("_describe");
    assertThat(stdout).contains("compdef _streamx streamx");
  }

  @Test
  void shouldIncludeCommandDescriptionsInZshScript() throws Exception {
    ProcessResult result = exec("completion", "zsh");

    result.assertSuccess();
    String stdout = result.stdout();
    // Subcommand descriptions from @CommandLine.Command header attributes
    assertThat(stdout).contains("Publish events");
    assertThat(stdout).contains("Modify StreamX settings");
    assertThat(stdout).contains("Operate local StreamX instance");
    assertThat(stdout).contains("Generate shell completion scripts");
  }

  @Test
  void shouldIncludeSubcommandFunctionsInZshScript() throws Exception {
    ProcessResult result = exec("completion", "zsh");

    result.assertSuccess();
    String stdout = result.stdout();
    assertThat(stdout).contains("_streamx_publish()");
    assertThat(stdout).contains("_streamx_settings()");
    assertThat(stdout).contains("_streamx_local()");
    assertThat(stdout).contains("_streamx_publish_events()");
    assertThat(stdout).contains("_streamx_publish_event()");
    assertThat(stdout).contains("_streamx_publish_stream()");
  }

  @Test
  void shouldIncludeOptionDescriptionsInZshScript() throws Exception {
    ProcessResult result = exec("completion", "zsh");

    result.assertSuccess();
    String stdout = result.stdout();
    assertThat(stdout).contains("Print debug information");
    assertThat(stdout).contains("StreamX ingestion URL");
    assertThat(stdout).contains("Continue even if some event publish failed");
  }
}
