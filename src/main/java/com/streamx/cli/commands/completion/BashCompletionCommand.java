package com.streamx.cli.commands.completion;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.CommonOptions;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

@CommandLine.Command(
    name = "bash",
    header = "Generate bash completion script (use --help for setup instructions)",
    description = {
        "If installed via Homebrew, bash completions are set up automatically.",
        "The bash-completion package is required:",
        "  brew install bash-completion@2",
        "Follow its caveats to configure your shell, then open a new terminal.",
        "",
        "Manual setup (without Homebrew):",
        "  streamx completion bash > /etc/bash_completion.d/streamx",
        "",
        "To load completions in the current session only:",
        "  source <(streamx completion bash)"
    }
)
public class BashCompletionCommand extends AbstractCommand<String> {

  @Override
  public List<String> getHiddenOptions() {
    return List.of(CommonOptions.OUTPUT_LONG, CommonOptions.VERBOSE_LONG);
  }

  @Override
  public String getTextOutput(CommandResult<String> result) {
    return result.getData();
  }

  @Override
  public CommandResult<String> runCommand() {
    CommandSpec rootSpec = getRootCommandSpec();
    String script = BashCompletionGenerator.generate(
        rootSpec.name(),
        rootSpec.commandLine()
    );
    return new CommandResult<>(script);
  }

  protected CommandSpec getRootCommandSpec() {
    CommandSpec current = spec;
    while (current.parent() != null) {
      current = current.parent();
    }
    return current;
  }
}
