package com.streamx.cli.framework;

import static com.streamx.cli.i18n.MessageProvider.msg;

import java.io.PrintWriter;
import picocli.CommandLine;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.UnmatchedArgumentException;

public class ShortErrorMessageHandler implements IParameterExceptionHandler {

  @Override
  public int handleParseException(ParameterException ex, String[] args) {
    CommandLine cmd = ex.getCommandLine();
    return shortErrorMessage(ex, cmd);
  }

  static int shortErrorMessage(Exception ex, CommandLine cmd) {
    PrintWriter writer = cmd.getErr();

    if (
        ex instanceof ParameterException
        || ex instanceof IllegalArgumentException
        || ex instanceof CliException
    ) {
      writer.println(cmd.getColorScheme().errorText(ex.getMessage()));
    } else {
      String errorMessage = """
          ❌ Something went wrong while running the command.
          
          Please try again with `--verbose` for more details.
          If the problem persists, report it here:
          https://github.com/streamx-com/streamx-cli/issues
          
          You can also contact us at:
          https://www.streamx.dev/contact-us.html
          """.strip();

      writer.println(cmd.getColorScheme().errorText(errorMessage));
    }

    if (ex instanceof ParameterException) {
      UnmatchedArgumentException.printSuggestions((ParameterException) ex, writer);
    }

    if (ex instanceof ParameterException || ex instanceof IllegalArgumentException) {
      CommandSpec spec = cmd.getCommandSpec();
      writer.printf(msg.tryForMoreInformationOnAvailableOptions(
          spec.qualifiedName(),
          "help".equals(spec.name()) ? "" : " --help"
      ));
      return cmd.getCommandSpec().exitCodeOnInvalidInput();
    }
    return cmd.getCommandSpec().exitCodeOnExecutionException();
  }

}
