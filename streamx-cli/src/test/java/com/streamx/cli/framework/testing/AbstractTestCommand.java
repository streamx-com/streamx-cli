package com.streamx.cli.framework.testing;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.ThrowingFunction;
import com.streamx.cli.framework.ThrowingFunction1;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

// Helper class for testing AbstractCommand
public class AbstractTestCommand<ResultT> extends AbstractCommand<ResultT> {
  public ThrowingFunction<CommandResult<ResultT>, CliException> runCommandHandler;
  public Supplier<List<String>> hiddenOptionsHandler;
  public ThrowingFunction1<CommandResult<ResultT>, String, CliException> getTextOutputHandler;

  public void setRunCommandHandler(ThrowingFunction<CommandResult<ResultT>, CliException> handler) {
    this.runCommandHandler = handler;
  }

  public void setHiddenOptionsHandler(Supplier<List<String>> handler) {
    this.hiddenOptionsHandler = handler;
  }

  public void setGetTextOutputHandler(
      ThrowingFunction1<CommandResult<ResultT>, String, CliException> handler
  ) {
    this.getTextOutputHandler = handler;
  }

  @Override
  public CommandResult<ResultT> runCommand() throws CliException {
    if (runCommandHandler != null) {
      return runCommandHandler.get();
    }
    throw new CliException("No run command handler set");
  }

  @Override
  public List<String> getHiddenOptions() {
    if (hiddenOptionsHandler != null) {
      return hiddenOptionsHandler.get();
    }
    return super.getHiddenOptions();
  }

  @Override
  public String getTextOutput(CommandResult<ResultT> result) throws CliException {
    if (getTextOutputHandler != null) {
      return getTextOutputHandler.get(result);
    }
    return super.getTextOutput(result);
  }

  @Override
  protected Terminal createTerminal() throws IOException {
    return TerminalBuilder.builder()
      .system(false)
      .streams(System.in, System.out)
      .dumb(true)
      .build();
  }
}
