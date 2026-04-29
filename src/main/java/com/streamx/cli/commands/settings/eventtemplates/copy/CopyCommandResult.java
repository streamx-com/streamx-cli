package com.streamx.cli.commands.settings.eventtemplates.copy;

public record CopyCommandResult(
    String sourceId,
    String destId,
    String path
) {
}
