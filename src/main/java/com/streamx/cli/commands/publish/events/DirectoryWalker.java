package com.streamx.cli.commands.publish.events;

import static com.streamx.cli.commands.publish.events.TemplateLoader.EVENTTEMPLATE_FILE;
import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.commands.publish.AbortStreamException;
import com.streamx.cli.commands.publish.EventTemplateProcessor;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.ingestion.CloudEventsSerde;
import com.streamx.clients.ingestion.exceptions.StreamxClientException;
import com.streamx.clients.ingestion.publisher.Publisher;
import io.cloudevents.CloudEvent;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class DirectoryWalker {
  private final PublishingTracker tracker;
  private final Publisher publisher;
  private final int batchSize;
  private final boolean continueOnError;

  private final DebugDirectoryWriter debugDirectoryWriter;
  private final boolean dryRun;

  private final List<CloudEvent> batch = new ArrayList<>();
  private final List<TemplateContext> batchContexts = new ArrayList<>();

  DirectoryWalker(
      PublishingTracker tracker,
      Publisher publisher,
      int batchSize,
      boolean continueOnError,
      DebugDirectoryWriter debugDirectoryWriter,
      boolean dryRun
  ) {
    this.tracker = tracker;
    this.publisher = publisher;
    this.batchSize = batchSize;
    this.continueOnError = continueOnError;
    this.debugDirectoryWriter = debugDirectoryWriter;
    this.dryRun = dryRun;
  }

  void walk(Path rootPath, TemplateContext rootContext) {
    processDirectory(rootPath, rootPath, rootContext);
  }

  void flushBatch() {
    if (batch.isEmpty()) {
      return;
    }
    TemplateContext ctx = batchContexts.getFirst();
    List<CloudEvent> toSend = new ArrayList<>(batch);
    batch.clear();
    batchContexts.clear();
    sendBatch(toSend, ctx);
  }

  private void processDirectory(Path dir, Path rootPath, TemplateContext ctx) {
    if (!dir.equals(rootPath)) {
      Path localTemplateFile = dir.resolve(EVENTTEMPLATE_FILE);
      if (Files.exists(localTemplateFile)) {
        JsonNode localTemplate = TemplateLoader.load(localTemplateFile, dir);
        ctx = new TemplateContext(localTemplate, localTemplateFile.toString(), null, null);
      }
    }

    List<Path> payloadFiles = new ArrayList<>();
    List<Path> subDirs = new ArrayList<>();

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      for (Path entry : stream) {
        if (Files.isDirectory(entry)) {
          subDirs.add(entry);
        } else if (!isSkippedFile(entry)) {
          payloadFiles.add(entry);
        }
      }
    } catch (IOException e) {
      throw new CliException(msg.unableToPublishStream(dir.toString()), e);
    }

    if (ctx != null) {
      for (Path payloadFile : payloadFiles) {
        processPayloadFile(payloadFile, rootPath, ctx);
      }
    }

    for (Path subDir : subDirs) {
      processDirectory(subDir, rootPath, ctx);
    }
  }

  private boolean isSkippedFile(Path file) {
    String name = file.getFileName().toString();
    return EVENTTEMPLATE_FILE.equals(name)
        || (name.startsWith(".") && name.endsWith(EVENTTEMPLATE_FILE));
  }

  private void processPayloadFile(Path payloadPath, Path rootPath, TemplateContext ctx) {
    int eventNumber = (batchSize <= 1) ? tracker.nextEventNumber() : 0;

    CloudEvent cloudEvent;
    try {
      cloudEvent = new EventTemplateProcessor(
          ctx.template().toString(), payloadPath, rootPath, null).toCloudEvent();
    } catch (Exception e) {
      String errorMessage = msg.eventPublishFailed(
          String.valueOf(eventNumber), "''", payloadPath.toString(), e.getMessage());
      tracker.recordFailure("''", payloadPath.toString(),
          ctx.templatePath(), ctx.appliedPatch(), errorMessage);
      System.err.println(errorMessage);
      if (!continueOnError) {
        throw new AbortStreamException(new CliException(errorMessage));
      }
      return;
    }

    if (debugDirectoryWriter != null) {
      try {
        debugDirectoryWriter.writeEvent(
            payloadPath,
            ctx,
            CloudEventsSerde.toJson(cloudEvent)
        );
      } catch (IOException e) {
        System.err.println(
            msg.failedToWriteDebugArtefacts(
                payloadPath.toString(),
                e.getMessage()
            )
        );
      }
    }

    if (dryRun) {
      tracker.recordSuccess();
      System.err.println(msg.eventPublished(
          String.valueOf(tracker.currentEventNumber()),
          cloudEvent.getType(), cloudEvent.getSubject()));
      return;
    }

    if (batchSize > 1) {
      batch.add(cloudEvent);
      batchContexts.add(ctx);
      if (batch.size() >= batchSize) {
        TemplateContext batchCtx = batchContexts.getFirst();
        List<CloudEvent> toSend = new ArrayList<>(batch);
        batch.clear();
        batchContexts.clear();
        try {
          sendBatch(toSend, batchCtx);
        } catch (CliException e) {
          if (!continueOnError) {
            throw new AbortStreamException(e);
          }
        }
      }
    } else {
      try {
        sendEvent(eventNumber, cloudEvent, ctx);
      } catch (CliException e) {
        if (!continueOnError) {
          throw new AbortStreamException(e);
        }
      }
    }
  }

  private void sendEvent(int eventNumber, CloudEvent event, TemplateContext ctx) {
    try {
      publisher.send(List.of(event));
      tracker.recordSuccess();

      System.err.println(msg.eventPublished(
          String.valueOf(eventNumber), event.getType(), event.getSubject()));
    } catch (StreamxClientException e) {
      tracker.recordFailure(
          event.getType(), event.getSubject(), ctx.templatePath(), ctx.appliedPatch(),
          e.getMessage());

      String errorMessage = msg.eventPublishFailed(
          String.valueOf(eventNumber), event.getType(), event.getSubject(), e.getMessage());
      System.err.println(errorMessage);

      throw new CliException(errorMessage);
    }
  }

  private void sendBatch(List<CloudEvent> events, TemplateContext ctx) {
    int batchNumber = tracker.nextBatchNumber();
    try {
      publisher.send(events);
      tracker.recordBatchSuccess(events);

      System.err.println(msg.batchPublished(
          String.valueOf(batchNumber), String.valueOf(events.size())));
    } catch (StreamxClientException e) {
      tracker.recordBatchFailure(events, ctx.templatePath(), ctx.appliedPatch(), e.getMessage());

      String errorMessage = msg.batchPublishFailed(
          String.valueOf(batchNumber), String.valueOf(events.size()), e.getMessage());
      System.err.println(errorMessage);

      throw new CliException(errorMessage);
    }
  }
}