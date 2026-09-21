package com.streamx.cli.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.test.CliBaseIT;
import org.junit.jupiter.api.Test;

class StreamxCommandIT extends CliBaseIT {

  @Test
  void shouldPrintHelpInformation() throws Exception {
    ProcessResult result = exec();

    assertThat(result.stdout()).contains("StreamX CLI. More info at");
    assertThat(result.stderr()).isEmpty();
  }
}
