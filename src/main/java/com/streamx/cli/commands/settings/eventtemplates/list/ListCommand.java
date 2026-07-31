package com.streamx.cli.commands.settings.eventtemplates.list;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import com.streamx.cli.commands.publish.event.EventTemplateCatalog.TemplateLocation;
import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.TextTable;
import java.util.Arrays;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    header = "Display all available event templates",
    description = "Lists every template that `publish event` can resolve, along with its "
        + "CloudEvent type, source (default / custom / registered in settings), "
        + "and path on disk.",
    footer = {
        "",
        "Examples:",
        "  streamx settings event-templates list",
        "  streamx settings event-templates list -o json",
        "  streamx settings event-templates list -o yaml | yq '.templates[].id'"
    }
)
public class ListCommand extends AbstractCommand<ListCommandResult> {

  @Override
  public CommandResult<ListCommandResult> runCommand() {
    List<TemplateLocation> templates = EventTemplateCatalog.listAll();
    String home = StreamxHome.getStreamxHome().toAbsolutePath().toString();
    return new CommandResult<>(new ListCommandResult(home, templates));
  }

  @Override
  public String getTextOutput(CommandResult<ListCommandResult> result) {
    List<TemplateLocation> templates = result.getData().templates();
    if (templates.isEmpty()) {
      return msg.eventTemplatesNoTemplatesFound();
    }

    return TextTable.render(
        List.of("TEMPLATE ID", "TYPE", "DEFINED AT", "PATH"),
        templates.stream()
            .map(t -> Arrays.asList(t.id(), t.type(), t.source(), t.path()))
            .toList());
  }
}
