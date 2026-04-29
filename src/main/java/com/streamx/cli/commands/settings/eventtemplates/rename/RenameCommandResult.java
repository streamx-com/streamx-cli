package com.streamx.cli.commands.settings.eventtemplates.rename;

public record RenameCommandResult(
    String oldId,
    String newId,
    String source,
    String path
) {
}
