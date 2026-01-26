package com.streamx.cli.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamx.cli.framework.testing.AbstractCommandBaseTest;
import com.streamx.cli.framework.testing.AbstractTestCommand;
import com.streamx.cli.framework.testing.TestObject;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class AbstractCommandTest extends AbstractCommandBaseTest {
  @Test
  void shouldExecuteSuccessfully() {
    AbstractTestCommand<TestObject> command = new AbstractTestCommand<>();
    command.setRunCommandHandler(() -> new CommandResult<>(TestObject.random()));
    new CommandLine(command);
    int exitCode = command.execute();

    assertEquals(0, exitCode);
    assertFalse(outStream.toString().isEmpty());
    assertTrue(errStream.toString().isEmpty());
  }

  @Test
  void shouldHandleExceptionGracefully() {
    AbstractTestCommand<TestObject> command = new AbstractTestCommand<>();
    command.setRunCommandHandler(() -> {
      throw new CliException("Test exception");
    });
    new CommandLine(command);
    int exitCode = command.execute();

    assertEquals(1, exitCode);
    assertTrue(outStream.toString().isEmpty());
    assertTrue(errStream.toString().contains("Test exception"));
  }

  @Test
  void shouldHideOptionsBasedOnHandler() {
    AbstractTestCommand<Void> command1 = new AbstractTestCommand<>();
    new CommandLine(command1);

    assertNotNull(command1.spec.findOption(CommonOption.OUTPUT_LONG));

    AbstractTestCommand<Void> command2 = new AbstractTestCommand<>();
    command2.setHiddenOptionsHandler(() -> List.of(CommonOption.OUTPUT_LONG));
    new CommandLine(command2);

    assertNull(command2.spec.findOption(CommonOption.OUTPUT_LONG));
  }

  @Test
  void shouldBeAbleToPromptForInput() {
    AbstractTestCommand<Void> command = new AbstractTestCommand<>();
    CommandLine commandLine = new CommandLine(command);
    command.setSpec(commandLine.getCommandSpec());

    String input1 = "first\n";
    System.setIn(new ByteArrayInputStream(input1.getBytes()));
    String result1;
    try {
      result1 = command.promptForInput("Enter first:", null);
    } catch (CliException e) {
      throw new RuntimeException(e);
    }

    assertEquals("first", result1);

    String input2 = "second\n";
    System.setIn(new ByteArrayInputStream(input2.getBytes()));
    String result2;
    try {
      result2 = command.promptForInput("Enter second:", null);
    } catch (CliException e) {
      throw new RuntimeException(e);
    }

    assertEquals("second", result2);
  }
}
