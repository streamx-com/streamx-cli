package com.streamx.cli.commands.completion;

import com.streamx.cli.commands.settings.eventtemplates.NonDefaultTemplateIdCompletionCandidates;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "__complete-non-default-template-ids",
    hidden = true,
    header = "Internal: list every event template ID the user can rename or delete, "
        + "one per line (defaults are excluded)"
)
public class CompleteNonDefaultTemplateIdsCommand extends AbstractCommand<List<String>> {

  @Override
  public CommandResult<List<String>> runCommand() {
    return new CommandResult<>(NonDefaultTemplateIdCompletionCandidates.loadIds());
  }

  @Override
  public String getTextOutput(CommandResult<List<String>> result) {
    return String.join("\n", result.getData());
  }
}
