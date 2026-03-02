package com.streamx.cli.commands.publish.stream;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.ingestion.CloudEventsSerde;
import com.streamx.cli.ingestion.ConcatenatedJsonSerde;
import com.streamx.cli.ingestion.IngestionClientConfig;
import com.streamx.cli.ingestion.IngestionClientPicocliOptions;
import com.streamx.cli.ingestion.StreamxClientFactory;
import com.streamx.clients.ingestion.StreamxClient;
import com.streamx.clients.ingestion.exceptions.StreamxClientException;
import com.streamx.clients.ingestion.publisher.Publisher;
import io.cloudevents.CloudEvent;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "stream",
    mixinStandardHelpOptions = true,
    header = "Publishes stream of events.")
public class StreamCommand extends AbstractCommand<StreamCommandResult> {

  @CommandLine.Mixin
  IngestionClientPicocliOptions ingestionOptions;

  @CommandLine.Parameters(
      index = "0",
      description = "Events source. It can be a file path or resource URI.",
      arity = "0..1",
      defaultValue = CommandLine.Parameters.NULL_VALUE
  )
  public String source;

  @CommandLine.Option(
      names = {"--chunk-size", "-c"},
      description = "Number of events per chunk (default: ${DEFAULT-VALUE}).",
      defaultValue = "100"
  )
  int chunkSize;

  @Override
  public CommandResult<StreamCommandResult> runCommand() {
    if (source != null) {
      SourceValidator.validate(source);
    }

    if (this.verbose) {
      System.out.println(msg.runningPublishStreamCommand());
      System.out.println(msg.resolvingStreamxClientConfig());
    }

    IngestionClientConfig ingestionClientConfig = ingestionOptions.getIngestionClientConfig();

    if (this.verbose) {
      System.out.println(msg.initializingStreamxClient());
      System.out.println(IngestionClientConfig.prettyPrint(ingestionClientConfig));
    }

    try (StreamxClient streamxClient = StreamxClientFactory.create(ingestionClientConfig)) {
      try {
        InputStream sourceStream = getSourceStream();

        Publisher publisher = streamxClient.newPublisher();

        try (Stream<JsonNode> jsonStream = ConcatenatedJsonSerde.parse(sourceStream)) {
          StreamPublishingTracker tracker = new StreamPublishingTracker();
          List<CloudEvent> chunk = new ArrayList<>();

          jsonStream
              .map(CloudEventsSerde::fromJson)
              .forEach(event -> {
                chunk.add(event);
                if (chunk.size() >= chunkSize) {
                  sendChunk(publisher, chunk, tracker);
                  chunk.clear();
                }
              });

          if (!chunk.isEmpty()) {
            sendChunk(publisher, chunk, tracker);
          }

          return new CommandResult<>(new StreamCommandResult(
              tracker.getSuccessCount(),
              tracker.getFailureCount(),
              tracker.getErrors()
          ));
        }
      } catch (Exception e) {
        throw new CliException(msg.unableToPublishStream(e.getMessage()), e);
      }
    } catch (StreamxClientException e) {
      throw new CliException(msg.unableToCreateStreamxClient(ingestionClientConfig.url()), e);
    }
  }

  @Override
  public String getTextOutput(CommandResult<StreamCommandResult> result) {
    StreamCommandResult data = result.getData();
    StringBuilder sb = new StringBuilder();

    int total = data.successCount() + data.failureCount();
    sb.append(msg.streamPublishingCompleted(
        total,
        data.successCount(),
        data.failureCount()
    )).append('\n');

    List<StreamCommandResult.EventError> errors = data.firstErrors();
    if (!errors.isEmpty()) {
      sb.append('\n');
      sb.append(msg.streamFirstErrors(errors.size())).append('\n');
      for (StreamCommandResult.EventError error : errors) {
        sb.append(msg.streamEventError(
            error.eventNumber(),
            error.type(),
            error.subject(),
            error.errorMessage()
        )).append('\n');
      }
      if (data.failureCount() > StreamCommandResult.MAX_STORED_ERRORS) {
        sb.append(msg.streamMoreErrorsNotShown(
            data.failureCount() - StreamCommandResult.MAX_STORED_ERRORS
        )).append('\n');
      }
    }

    return sb.toString();
  }

  private void sendChunk(
      Publisher publisher,
      List<CloudEvent> chunk,
      StreamPublishingTracker tracker
  ) {
    if (this.verbose) {
      System.out.println(msg.sendingChunk(chunk.size()));
    }

    for (CloudEvent event : chunk) {
      int eventNumber = tracker.nextEventNumber();
      try {
        publisher.send(List.of(event));
        tracker.recordSuccess();
        System.out.println(msg.eventPublished(
            String.valueOf(eventNumber),
            event.getType(),
            event.getSubject()
        ));
      } catch (Exception e) {
        tracker.recordFailure(
            event.getType(),
            event.getSubject(),
            e.getMessage());
        System.err.println(msg.eventPublishFailed(
            String.valueOf(eventNumber),
            event.getType(), event.getSubject(), e.getMessage()));
      }
    }
  }

  private InputStream getSourceStream() throws CliException {
    InputStream input;
    if (source != null) {
      try {
        URI uri = URI.create(source);
        if (uri.getScheme() != null) {
          input = uri.toURL().openStream();
        } else {
          input = java.nio.file.Files.newInputStream(java.nio.file.Path.of(source));
        }
      } catch (CliException e) {
        throw e;
      } catch (Exception e) {
        throw new CliException(msg.unableToOpenSourceInputStream(source), e);
      }
    } else if (System.console() != null) {
      System.err.println(msg.pasteJsonContent());
      input = System.in;
    } else {
      input = System.in;
    }

    try {
      int firstByte = input.read();
      if (firstByte == -1) {
        throw new CliException(msg.inputIsEmpty());
      }

      return new SequenceInputStream(
          new ByteArrayInputStream(new byte[]{(byte) firstByte}),
          input
      );
    } catch (CliException e) {
      throw e;
    } catch (Exception e) {
      throw new CliException(msg.unableToReadInputStream(e.getMessage()), e);
    }
  }
}