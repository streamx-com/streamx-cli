package com.streamx.cli.commands.publish.stream;

import java.util.List;

public record StreamCommandResult(
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
      String errorMessage
  ) {
  }

  public record BatchError(
      int batchNumber,
      int eventCount,
      String errorMessage
  ) {
  }
}
