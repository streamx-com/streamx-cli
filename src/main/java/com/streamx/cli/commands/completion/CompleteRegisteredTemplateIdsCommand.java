package com.streamx.cli.commands.completion;

import com.streamx.cli.commands.settings.eventtemplates.RegisteredTemplateIdCompletionCandidates;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.CommonOptions;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "__complete-registered-template-ids",
    hidden = true,
    header = "Internal: list every event template ID currently registered in settings, "
        + "one per line"
)
public class CompleteRegisteredTemplateIdsCommand extends AbstractCommand<List<String>> {

  @Override
  public List<String> getHiddenOptions() {
    return List.of(CommonOptions.OUTPUT_LONG, CommonOptions.VERBOSE_LONG);
  }

  @Override
  public CommandResult<List<String>> runCommand() {
    List<String> ids = new ArrayList<>();
    RegisteredTemplateIdCompletionCandidates.loadIds().forEach(ids::add);
    return new CommandResult<>(ids);
  }

  @Override
  public String getTextOutput(CommandResult<List<String>> result) {
    return String.join("\n", result.getData());
  }
}
