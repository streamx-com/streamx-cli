package com.streamx.cli.commands.settings.eventtemplates.list;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import com.streamx.cli.commands.publish.event.EventTemplateCatalog.TemplateLocation;
import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    header = "Display all available event templates",
    description = "Lists every template that `publish event` can resolve, along with its "
        + "CloudEvent type, source (default / user / registered), and path on disk.",
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

    int idWidth = Math.max(11, templates.stream()
        .mapToInt(t -> t.id() == null ? 0 : t.id().length())
        .max().orElse(0));
    int typeWidth = Math.max(4, templates.stream()
        .mapToInt(t -> t.type() == null ? 0 : t.type().length())
        .max().orElse(0));
    int sourceWidth = Math.max(10, templates.stream()
        .mapToInt(t -> t.source() == null ? 0 : t.source().length())
        .max().orElse(0));

    StringBuilder sb = new StringBuilder();
    sb.append(msg.eventTemplatesListHeader()).append("\n");
    sb.append(String.format(
        "%-" + idWidth + "s  %-" + typeWidth + "s  %-" + sourceWidth + "s  %s%n",
        "TEMPLATE ID", "TYPE", "DEFINED AT", "PATH"));
    for (TemplateLocation t : templates) {
      sb.append(String.format(
          "%-" + idWidth + "s  %-" + typeWidth + "s  %-" + sourceWidth + "s  %s%n",
          nullToDash(t.id()),
          nullToDash(t.type()),
          nullToDash(t.source()),
          nullToDash(t.path())));
    }
    return sb.toString().stripTrailing();
  }

  private static String nullToDash(String s) {
    return s == null ? "-" : s;
  }
}
