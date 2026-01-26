package com.streamx.cli.framework;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamx.cli.framework.testing.AbstractCommandBaseTest;
import com.streamx.cli.framework.testing.AbstractTestCommand;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class AbstractCommandVerboseOptionTest extends AbstractCommandBaseTest {
  @Test
  void shouldPrintStackTraceIfProvided() {
    AbstractTestCommand<Void> command = new AbstractTestCommand<>();
    command.setRunCommandHandler(() -> {
      throw new CliException("Test exception");
    });
    CommandLine commandLine = new CommandLine(command);
    commandLine.parseArgs(CommonOption.VERBOSE_LONG);

    command.execute();

    assertTrue(
        errStream.toString().contains("com.streamx.cli.framework.CliException: Test exception")
    );
  }

  @Test
  void shouldNotPrintStackTraceByDefault() {
    AbstractTestCommand<Void> command = new AbstractTestCommand<>();
    command.setRunCommandHandler(() -> {
      throw new CliException("Test exception");
    });
    new CommandLine(command);
    command.execute();

    assertTrue(errStream.toString().contains("Test exception"));
    assertFalse(
        errStream.toString().contains("com.streamx.cli.framework.CliException: Test exception")
    );
  }
}
