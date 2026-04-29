package com.streamx.cli.commands.completion;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.CommonOption;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

@CommandLine.Command(
    name = "zsh",
    header = "Generate zsh completion script (use --help for setup instructions)",
    description = {
        "To load completions in the current session:",
        "  source <(streamx completion zsh)",
        "",
        "To load completions for every new session:",
        "  mkdir -p ~/.zsh/completions",
        "  streamx completion zsh > ~/.zsh/completions/_streamx",
        "",
        "Then add the following to ~/.zshrc (before compinit):",
        "  fpath=(~/.zsh/completions $$fpath)",
        "",
        "You may need to restart your shell or run 'exec zsh'",
        "for the changes to take effect."
    }
)
public class ZshCompletionCommand extends AbstractCommand<String> {

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
    String script = ZshCompletionGenerator.generate(
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
