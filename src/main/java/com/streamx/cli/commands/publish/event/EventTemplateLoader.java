package com.streamx.cli.commands.publish.event;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.commands.publish.event.EventTemplateCatalog.TemplateLocation;
import com.streamx.cli.framework.CliException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public class EventTemplateLoader {

  public static final String TEMPLATE_SETTINGS_MAPPING_PREFIX = "eventtemplate.";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  record TemplateDescriptor(String template, String templatePath) {}

  public TemplateDescriptor load(@NotNull String templateId) {
    TemplateLocation location = EventTemplateCatalog.findById(templateId)
        .orElseThrow(() -> new CliException(msg.eventTemplateNotFound(templateId)));

    Path file = Path.of(location.path());
    String content;
    try {
      content = Files.readString(file);
    } catch (IOException e) {
      throw new CliException(msg.eventTemplateFileMissing(location.path()), e);
    }

    validateTemplate(content, templateId);
    return new TemplateDescriptor(content, location.path());
  }

  private void validateTemplate(String template, String templateId) {
    try {
      MAPPER.readTree(template);
    } catch (Exception e) {
      throw new CliException(msg.invalidEventTemplate(templateId));
    }
  }
}
