package com.streamx.cli.framework;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamx.cli.framework.testing.AbstractCommandBaseTest;
import com.streamx.cli.framework.testing.AbstractTestCommand;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class AbstractCommandVerboseOptionTest extends AbstractCommandBaseTest {
  @Test
  void ifProvided_printsStackTrace() {
    var command = new AbstractTestCommand<>();
    command.setRunCommandHandler(() -> {
      throw new RuntimeException("Test exception");
    });
    var commandLine = new CommandLine(command);
    commandLine.parseArgs(CommonOption.VERBOSE_LONG);

    command.execute();

    assertTrue(errStream.toString().contains("java.lang.RuntimeException: Test exception"));
  }

  @Test
  void ifNotProvided_doesntPrintStackTrace() {
    var command = new AbstractTestCommand<>();
    command.setRunCommandHandler(() -> {
      throw new RuntimeException("Test exception");
    });
    new CommandLine(command);
    command.execute();

    assertFalse(errStream.toString().contains("java.lang.RuntimeException: Test exception"));
  }
}
