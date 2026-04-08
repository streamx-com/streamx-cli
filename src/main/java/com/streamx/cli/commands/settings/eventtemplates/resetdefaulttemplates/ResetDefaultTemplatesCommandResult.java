package com.streamx.cli.commands.settings.eventtemplates.resetdefaulttemplates;

import java.util.List;

public record ResetDefaultTemplatesCommandResult(
    String path,
    List<String> templates
) {
}
