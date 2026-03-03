package com.streamx.cli.commands.publish.stream;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.publish.stream.StreamCommandResult.EventError;
import java.util.ArrayList;
import java.util.List;

class StreamPublishingTracker {

  private int successCount;
  private int failureCount;
  private final List<EventError> errors = new ArrayList<>();

  /** Set limit for stored error details to avoid OOM
  when publishing very large streams of events. */
  public static final int MAX_STORED_ERRORS = 1000;

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
    if (errors.size() < MAX_STORED_ERRORS) {
      errors.add(new EventError(currentEventNumber(), type, subject, errorMessage));
    }
  }

  int currentEventNumber() {
    return successCount + failureCount;
  }

  int nextEventNumber() {
    return currentEventNumber() + 1;
  }

  public String toSummary() {
    StringBuilder summary = new StringBuilder();

    int total = successCount + failureCount;
    summary.append(msg.streamPublishingCompleted(
        total,
        successCount,
        failureCount
    )).append('\n');

    if (!errors.isEmpty()) {
      summary.append('\n');
      summary.append(msg.streamFirstErrors(errors.size())).append('\n');
      for (StreamCommandResult.EventError error : errors) {
        summary.append(msg.streamEventError(
            error.eventNumber(),
            error.type(),
            error.subject(),
            error.errorMessage()
        )).append('\n');
      }

      if (failureCount > MAX_STORED_ERRORS) {
        summary.append(msg.streamMoreErrorsNotShown(
            failureCount - MAX_STORED_ERRORS
        )).append('\n');
      }
    }

    return summary.toString();
  }
}
