package com.streamx.cli.commands.completion;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.CommonOption;
import java.util.List;
import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

@CommandLine.Command(
    name = "bash",
    header = "Generate bash completion script (use --help for setup instructions)",
    description = {
        "To load completions in the current session:",
        "  source <(streamx completion bash)",
        "",
        "To load completions for every new session:",
        "  streamx completion bash > /etc/bash_completion.d/streamx",
        "",
        "If using Homebrew (macOS or Linux):",
        "  streamx completion bash > $$(brew --prefix)/etc/bash_completion.d/streamx",
        "",
        "You may need to restart your shell for the changes to take effect.",
        "Note: bash-completion package must be installed."
    }
)
public class BashCompletionCommand extends AbstractCommand<String> {

  @Override
  public List<String> getHiddenOptions() {
    return List.of(CommonOption.OUTPUT_LONG, CommonOption.VERBOSE_LONG);
  }

  @Override
  public String getTextOutput(CommandResult<String> result) {
    return result.getData();
  }

  @Override
  public CommandResult<String> runCommand() {
    CommandSpec rootSpec = getRootCommandSpec();
    String script = AutoComplete.bash(
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
