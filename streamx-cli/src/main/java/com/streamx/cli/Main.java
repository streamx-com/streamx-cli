package com.streamx.cli;

import com.streamx.cli.framework.AbstractCommandGroup;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.ShortErrorMessageHandler;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(
    name = "streamx",
    mixinStandardHelpOptions = true,
    description = "StreamX CLI. More info at https://streamx.dev",
    subcommands = {}
)
public class Main extends AbstractCommandGroup {
  @CommandLine.Spec
  CommandLine.Model.CommandSpec commandSpec;

  @Override
  public CommandResult<Void> runCommand() {
    commandSpec
      .commandLine()
      .setParameterExceptionHandler(new ShortErrorMessageHandler())
        .usage(System.out);

    return new CommandResult<>(null);
  }
}
