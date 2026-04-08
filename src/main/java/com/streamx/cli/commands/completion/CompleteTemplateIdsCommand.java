package com.streamx.cli.commands.completion;

import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.CommonOptions;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "__complete-template-ids",
    hidden = true,
    header = "Internal: list every event template ID, one per line"
)
public class CompleteTemplateIdsCommand extends AbstractCommand<List<String>> {

  @Override
  public List<String> getHiddenOptions() {
    return List.of(CommonOptions.OUTPUT_LONG, CommonOptions.VERBOSE_LONG);
  }

  @Override
  public CommandResult<List<String>> runCommand() {
    return new CommandResult<>(EventTemplateCatalog.templateIds());
  }

  @Override
  public String getTextOutput(CommandResult<List<String>> result) {
    return String.join("\n", result.getData());
  }
}
