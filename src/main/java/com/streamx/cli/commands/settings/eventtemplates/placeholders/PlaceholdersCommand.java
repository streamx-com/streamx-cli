package com.streamx.cli.commands.settings.eventtemplates.placeholders;

import com.streamx.cli.commands.publish.EventTemplatePlaceholders;
import com.streamx.cli.commands.publish.EventTemplatePlaceholders.Placeholder;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "placeholders",
    header = "List the placeholders that may be used inside event templates",
    description = "Prints every placeholder that `publish event` substitutes when rendering an "
        + "event template, along with a short description of what each one resolves to.",
    footer = {
        "",
        "Examples:",
        "  streamx settings event-templates placeholders",
        "  streamx settings event-templates placeholders -o json",
        "  streamx settings event-templates placeholders -o yaml"
    }
)
public class PlaceholdersCommand extends AbstractCommand<List<Placeholder>> {

  @Override
  public CommandResult<List<Placeholder>> runCommand() {
    return new CommandResult<>(EventTemplatePlaceholders.all());
  }

  @Override
  public String getTextOutput(CommandResult<List<Placeholder>> result) {
    List<Placeholder> placeholders = result.getData();
    int nameWidth = placeholders.stream()
        .mapToInt(p -> p.name().length())
        .max().orElse(0);

    StringBuilder sb = new StringBuilder();
    for (Placeholder p : placeholders) {
      sb.append(String.format("%-" + nameWidth + "s  %s%n", p.name(), p.description()));
    }
    return sb.toString().stripTrailing();
  }
}
