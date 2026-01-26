package com.streamx.cli.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.streamx.cli.framework.testing.AbstractCommandBaseTest;
import com.streamx.cli.framework.testing.AbstractSilentTestCommand;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class AbstractSilentCommandTest extends AbstractCommandBaseTest {
  @Test
  void shouldNotHaveOutputOption() {
    AbstractSilentTestCommand command = new AbstractSilentTestCommand();
    new CommandLine(command); // Trigger all PicocLi initialization

    assertNull(command.spec.findOption(CommonOption.OUTPUT_LONG));
  }

  @Test
  void shouldReturnEmptyOutput() {
    AbstractSilentTestCommand command = new AbstractSilentTestCommand();
    command.setRunCommandHandler(() -> new CommandResult<>(null));
    command.execute();

    assertEquals("", outStream.toString());
    assertEquals("", errStream.toString());
  }
}
