package com.streamx.cli.commands.completion;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.CommonOptions;
import java.util.List;
import picocli.AutoComplete;
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

  private static final String ZSH_PREAMBLE = """
      #compdef streamx
      autoload -U +X bashcompinit && bashcompinit
      autoload -U +X compinit && compinit
      """;

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
    String bashScript = AutoComplete.bash(
        rootSpec.name(),
        rootSpec.commandLine()
    );
    String zshScript = ZSH_PREAMBLE + bashScript;
    return new CommandResult<>(zshScript);
  }

  private CommandSpec getRootCommandSpec() {
    CommandSpec current = spec;
    while (current.parent() != null) {
      current = current.parent();
    }
    return current;
  }
}
