package com.streamx.cli.commands.publish.events;

import java.util.List;

public record EventsCommandResult(
    int successCount,
    int failureCount,
    int unknownCount,
    List<EventError> eventErrors,

    int batchSuccessCount,
    int batchFailureCount,
    List<BatchError> batchErrors
) {

    public record EventError(
        Integer eventNumber,
        Integer batchNumber,
        String type,
        String subject,
        String templatePath,
        String appliedPatch,
        String errorMessage
    ) {}

    public record BatchError(
        int batchNumber,
        int eventCount,
        String templatePath,
        String appliedPatch,
        String errorMessage
    ) {}
}