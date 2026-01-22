package dev.streamx.cli.framework;

import dev.streamx.cli.framework.cli.AbstractCommandGroup;
import dev.streamx.cli.framework.cli.CommandResult;
import dev.streamx.cli.framework.cli.ShortErrorMessageHandler;
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
  public CommandResult<Void> runCommand() throws RuntimeException {
    commandSpec
      .commandLine()
      .setParameterExceptionHandler(new ShortErrorMessageHandler())
      .usage(System.out);

    return new CommandResult<>(null);
  }
}
