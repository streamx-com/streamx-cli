package com.streamx.cli.commands.settings.eventtemplates.create;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import com.streamx.cli.commands.publish.event.EventTemplateCatalog.TemplateLocation;
import com.streamx.cli.commands.publish.event.UserEventTemplates;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.InteractivePicker;
import com.streamx.cli.framework.InteractivePicker.Session;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import picocli.CommandLine;

@CommandLine.Command(
    name = "create",
    header = "Create a new event template (interactive wizard)",
    description = "Prompts for a template ID and a CloudEvent type, then writes a starter "
        + "template to <streamxHome>/event-templates/custom/<id>.json. Run `edit` afterwards to "
        + "customize the content.",
    footer = {
        "",
        "Examples:",
        "  streamx settings event-templates create"
    }
)
public class CreateCommand extends AbstractCommand<CreateCommandResult> {

  private static final String STARTER_TEMPLATE = """
      {
        "specversion": "1.0",
        "id": "${uuid}",
        "source": "streamx-cli",
        "type": "%s",
        "datacontenttype": "application/json",
        "subject": "${payloadPath}",
        "time": "${currentTime}",
        "data": {
          "content": "file://${payloadPath}"
        }
      }
      """;

  @Override
  public CommandResult<CreateCommandResult> runCommand() {
    try (Session session = InteractivePicker.open()) {
      String id = promptForAvailableId(session);

      List<String> existingTypes = collectExistingTypes();
      String type = session.pick(msg.eventTemplateCreatePromptType(), existingTypes);
      if (type == null || type.isBlank()) {
        throw new CliException(msg.eventTemplateTypeRequired());
      }

      Path destination = UserEventTemplates.resolve(id);
      String content = STARTER_TEMPLATE.formatted(type);
      try {
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, content);
      } catch (IOException e) {
        throw new CliException(
            msg.failedToCreateEventTemplate(
                destination.toAbsolutePath().toString(), e.getMessage()),
            e);
      }

      return new CommandResult<>(new CreateCommandResult(
          id, type, destination.toAbsolutePath().toString()));
    }
  }

  private String promptForAvailableId(Session session) {
    while (true) {
      String id = session.pick(msg.eventTemplateCreatePromptId(), null);
      if (id == null || id.isBlank()) {
        throw new CliException(msg.eventTemplateIdRequired());
      }

      TemplateLocation existing = findExisting(id);
      if (existing == null) {
        return id;
      }

      System.err.println(
          msg.eventTemplateAlreadyExists(id, existing.path()));
      System.err.println(msg.eventTemplatePickDifferentId());
    }
  }

  private TemplateLocation findExisting(String id) {
    return EventTemplateCatalog.findById(id).orElseGet(() -> {
      Path destination = UserEventTemplates.resolve(id);
      if (Files.exists(destination)) {
        return new TemplateLocation(
            id,
            null,
            destination.toAbsolutePath().toString(),
            EventTemplateCatalog.SOURCE_CUSTOM
        );
      }
      return null;
    });
  }

  @Override
  public String getTextOutput(CommandResult<CreateCommandResult> result) {
    CreateCommandResult data = result.getData();
    return msg.eventTemplateCreated(data.id(), data.path());
  }

  private List<String> collectExistingTypes() {
    TreeSet<String> types = new TreeSet<>();
    for (TemplateLocation t : EventTemplateCatalog.listAll()) {
      if (t.type() != null && !t.type().isBlank()) {
        types.add(t.type());
      }
    }
    return List.copyOf(types);
  }
}
