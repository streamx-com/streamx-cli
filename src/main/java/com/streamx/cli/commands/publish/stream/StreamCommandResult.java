package com.streamx.cli.commands.publish.stream;

import java.util.List;

public record StreamCommandResult(
    int successCount,
    int failureCount,
    List<EventError> firstErrors
) {

  public static final int MAX_STORED_ERRORS = 100;

  public record EventError(
      int eventNumber,
      String type,
      String subject,
      String errorMessage
  ) {
  }
}