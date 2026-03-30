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
    assertThat(result.stdout()).contains("#compdef streamx");
  }
}
