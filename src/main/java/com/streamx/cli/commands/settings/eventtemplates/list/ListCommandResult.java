package com.streamx.cli.commands.settings.eventtemplates.list;

import com.streamx.cli.commands.publish.event.EventTemplateCatalog.TemplateLocation;
import java.util.List;

public record ListCommandResult(
    String streamxHome,
    List<TemplateLocation> templates
) {
}
