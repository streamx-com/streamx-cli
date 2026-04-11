package com.streamx.cli.commands.completion;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.CommonOptions;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

@CommandLine.Command(
    name = "zsh",
    header = "Generate zsh completion script (use --help for setup instructions)",
    description = {
        "If installed via Homebrew, zsh completions are set up automatically.",
        "New terminal sessions will have completions available out of the box.",
        "",
        "Manual setup (without Homebrew):",
        "  mkdir -p ~/.zsh/completions",
        "  streamx completion zsh > ~/.zsh/completions/_streamx",
        "",
        "Then add this to ~/.zshrc (before the line that runs compinit):",
        "  fpath=(~/.zsh/completions $$fpath)",
        "",
        "To load completions in the current session only:",
        "  source <(streamx completion zsh)"
    }
)
public class ZshCompletionCommand extends AbstractCommand<String> {

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
