package com.streamx.cli.commands.publish;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.annotation.DisabledIfDockerUnavailable;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
@DisabledIfDockerUnavailable
public class PublishCommandIT extends CliBaseIT {
  @Test
  void shouldPrintHelpInformation() throws Exception {
    ProcessResult result = exec("publish", "stream", "--help");

    assertThat(result.stdout()).contains("Publish events");
    assertThat(result.stderr()).isEmpty();
  }
}