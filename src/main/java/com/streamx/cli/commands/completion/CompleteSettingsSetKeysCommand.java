package com.streamx.cli.commands.completion;

import com.streamx.cli.commands.settings.SettingsSetKeyCompletionCandidates;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.CommonOptions;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "__complete-settings-set-keys",
    hidden = true,
    header = "Internal: list every settings key the user can pass to `settings set`"
)
public class CompleteSettingsSetKeysCommand extends AbstractCommand<List<String>> {

  @Override
  public List<String> getHiddenOptions() {
    return List.of(CommonOptions.OUTPUT_LONG, CommonOptions.VERBOSE_LONG);
  }

  @Override
  public CommandResult<List<String>> runCommand() {
    List<String> keys = new ArrayList<>();
    SettingsSetKeyCompletionCandidates.loadKeys().forEach(keys::add);
    return new CommandResult<>(keys);
  }

  @Override
  public String getTextOutput(CommandResult<List<String>> result) {
    return String.join("\n", result.getData());
  }
}
