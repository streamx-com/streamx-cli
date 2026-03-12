package com.streamx.cli.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@QuarkusTest
class StreamxCommandIT extends CliBaseIT {

  @Inject
  CommandLine.IFactory factory;

  @Test
  void allCommandsAndSubcommandsShouldExtendAbstractCommand() {
    CommandLine commandLine = new CommandLine(StreamxCommand.class, factory);

    Set<Class<?>> allCommandClasses = new HashSet<>();
    collectAllCommands(commandLine.getCommandSpec(), allCommandClasses);

    assertThat(allCommandClasses)
        .as("All commands and subcommands should extend AbstractCommand")
        .allSatisfy(commandClass ->
          assertThat(AbstractCommand.class.isAssignableFrom(commandClass))
            .as("Command %s should extend AbstractCommand", commandClass.getName())
            .isTrue()
        );
  }

  private void collectAllCommands(
      CommandLine.Model.CommandSpec commandSpec,
      Set<Class<?>> commands
  ) {
    Class<?> userObject = commandSpec.userObject().getClass();
    commands.add(userObject);

    for (CommandLine subcommand : commandSpec.subcommands().values()) {
      collectAllCommands(subcommand.getCommandSpec(), commands);
    }
  }

  @Test
  void shouldPrintHelpInformation() throws Exception {
    ProcessResult result = exec();

    assertThat(result.stdout()).contains("StreamX CLI. More info at");
    assertThat(result.stderr()).isEmpty();
  }
}
