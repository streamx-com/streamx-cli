package com.streamx.cli.commands.completion;

import com.streamx.cli.commands.settings.SettingsKeyCompletionCandidates;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "__complete-settings-keys",
    hidden = true,
    header = "Internal: list every settings key currently stored, one per line"
)
public class CompleteSettingsKeysCommand extends AbstractCommand<List<String>> {

  @Override
  public CommandResult<List<String>> runCommand() {
    List<String> keys = new ArrayList<>();
    SettingsKeyCompletionCandidates.loadKeys().forEach(keys::add);
    return new CommandResult<>(keys);
  }

  @Override
  public String getTextOutput(CommandResult<List<String>> result) {
    return String.join("\n", result.getData());
  }
}
