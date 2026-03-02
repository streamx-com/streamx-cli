package com.streamx.cli.commands.publish.stream;

import com.streamx.cli.commands.publish.stream.StreamCommandResult.EventError;
import java.util.ArrayList;
import java.util.List;

class StreamPublishingTracker {

  private int successCount;
  private int failureCount;
  private final List<EventError> errors = new ArrayList<>();

  public int getSuccessCount() {
    return successCount;
  }

  public int getFailureCount() {
    return failureCount;
  }

  public List<EventError> getErrors() {
    return errors;
  }


  void recordSuccess() {
    successCount++;
  }

  void recordFailure(String type, String subject, String errorMessage) {
    failureCount++;
    if (errors.size() < StreamCommandResult.MAX_STORED_ERRORS) {
      errors.add(new EventError(currentEventNumber(), type, subject, errorMessage));
    }
  }

  int currentEventNumber() {
    return successCount + failureCount;
  }

  int nextEventNumber() {
    return currentEventNumber() + 1;
  }
}
