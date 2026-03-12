package com.streamx.cli.commands.publish.stream;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.publish.stream.StreamCommandResult.BatchError;
import com.streamx.cli.commands.publish.stream.StreamCommandResult.EventError;
import io.cloudevents.CloudEvent;
import java.util.ArrayList;
import java.util.List;

class StreamPublishingTracker {

  private int successCount;
  private int failureCount;
  private int unknownCount;
  private final List<EventError> eventErrors = new ArrayList<>();

  private int batchSuccessCount;
  private int batchFailureCount;
  private final List<BatchError> batchErrors = new ArrayList<>();

  /** Set limit for stored error details to avoid OOM
   when publishing very large streams of events. */
  public static final int MAX_STORED_ERRORS = 1000;

  void recordSuccess() {
    successCount++;
  }

  void recordFailure(String type, String subject, String errorMessage) {
    failureCount++;
    if (eventErrors.size() < MAX_STORED_ERRORS) {
      eventErrors.add(new EventError(currentEventNumber(), null, type, subject, errorMessage));
    }
  }

  void recordBatchSuccess(List<CloudEvent> events) {
    batchSuccessCount++;
    successCount += events.size();
  }

  void recordBatchFailure(List<CloudEvent> events, String errorMessage) {
    int batchNumber = nextBatchNumber();
    batchFailureCount++;

    if (batchErrors.size() < MAX_STORED_ERRORS) {
      batchErrors.add(new BatchError(batchNumber, events.size(), errorMessage));
    }

    unknownCount += events.size();

    for (CloudEvent event : events) {
      if (eventErrors.size() < MAX_STORED_ERRORS) {
        eventErrors.add(new EventError(
            null,
            batchNumber,
            event.getType(),
            event.getSubject(),
            msg.eventPublishResultIsUnknown(batchNumber)
        ));
      }
    }
  }

  int currentEventNumber() {
    return successCount + failureCount + unknownCount;
  }

  int nextEventNumber() {
    return currentEventNumber() + 1;
  }

  int currentBatchNumber() {
    return batchSuccessCount + batchFailureCount;
  }

  int nextBatchNumber() {
    return currentBatchNumber() + 1;
  }

  boolean isBatchMode() {
    return batchSuccessCount + batchFailureCount > 0;
  }

  StreamCommandResult toResult() {
    return new StreamCommandResult(
        successCount,
        failureCount,
        unknownCount,
        eventErrors,
        batchSuccessCount,
        batchFailureCount,
        batchErrors
    );
  }

  public String toSummary() {
    StringBuilder summary = new StringBuilder();

    int total = successCount + failureCount + unknownCount;

    if (isBatchMode()) {
      int totalBatches = batchSuccessCount + batchFailureCount;

      summary.append(msg.streamBatchPublishingCompleted(
          total,
          successCount,
          failureCount,
          unknownCount,
          totalBatches,
          batchSuccessCount,
          batchFailureCount
      ));
    } else {
      summary.append(msg.streamPublishingCompleted(
          total,
          successCount,
          failureCount,
          unknownCount
      ));
    }

    if (!batchErrors.isEmpty()) {
      summary.append('\n');
      summary.append(msg.streamFirstBatchErrors(batchErrors.size()));

      for (BatchError error : batchErrors) {
        summary.append('\n');
        summary.append(msg.streamBatchError(
            error.batchNumber(),
            error.eventCount(),
            error.errorMessage()
        ));
      }

      if (batchFailureCount > MAX_STORED_ERRORS) {
        summary.append('\n');
        summary.append(msg.streamMoreErrorsNotShown(
            batchFailureCount - MAX_STORED_ERRORS
        ));
      }
    }

    if (!isBatchMode() && !eventErrors.isEmpty()) {
      summary.append('\n');
      summary.append(msg.streamFirstErrors(eventErrors.size()));

      for (EventError error : eventErrors) {
        summary.append('\n');
        summary.append(msg.streamEventError(
            error.eventNumber(),
            error.type(),
            error.subject(),
            error.errorMessage()
        )).append('\n');
      }

      if (failureCount > MAX_STORED_ERRORS) {
        summary.append('\n');
        summary.append(msg.streamMoreErrorsNotShown(
            failureCount - MAX_STORED_ERRORS
        ));
      }
    }

    return summary.toString();
  }
}