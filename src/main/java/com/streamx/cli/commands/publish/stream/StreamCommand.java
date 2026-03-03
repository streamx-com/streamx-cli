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

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "stream",
    mixinStandardHelpOptions = true,
    header = "Publishes stream of events")
public class StreamCommand extends AbstractCommand<StreamCommandResult> {

  @CommandLine.Mixin
  IngestionClientPicocliOptions ingestionOptions;

  @CommandLine.Parameters(
      index = "0",
      description = "Events source. It can be a file path or resource URI",
      arity = "0..1",
      defaultValue = CommandLine.Parameters.NULL_VALUE
  )
  public String source;

  @CommandLine.Option(
      names = {"--continue-on-error", "-x"},
      description = "Continue even if some event publish failed"
  )
  public boolean continueOnError;

  StreamPublishingTracker tracker = new StreamPublishingTracker();

  CommandResult<StreamCommandResult> prepareResult() {
    CommandResult<StreamCommandResult> result = new CommandResult<>(new StreamCommandResult(
        tracker.getSuccessCount(),
        tracker.getFailureCount(),
        tracker.getErrors()
    ));

    boolean someEventsFailed = !tracker.getErrors().isEmpty();
    if (someEventsFailed) {
      result.setError(new CliException(msg.eventsPartiallyFailedToPublish()));
    }

    if (someEventsFailed && !continueOnError) {
      result.setExitCodeOverride(1);
    } else {
      result.setExitCodeOverride(0);
    }

    return result;
  }

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

    InputStream sourceStream = SourceStream.get(source);

    try (StreamxClient streamxClient = StreamxClientFactory.create(ingestionClientConfig)) {
      try {
        try (Stream<JsonNode> jsonStream = ConcatenatedJsonSerde.parse(sourceStream)) {
          Publisher publisher = streamxClient.newPublisher();

          jsonStream
              .map(json -> {
                try {
                  return CloudEventsSerde.fromJson(json);
                } catch (CliException e) {
                  int eventNumber = tracker.nextEventNumber();
                  String errorMessage = msg.eventPublishFailed(
                      String.valueOf(eventNumber), "-", "-", e.getMessage()
                  );
                  tracker.recordFailure("-", "-", errorMessage);
                  System.err.println(errorMessage);

                  if (!continueOnError) {
                    throw new AbortStreamException(e);
                  }
                }
                return null;
              })
              .filter(Objects::nonNull)
              .forEach(event -> {
                try {
                  sendEvent(publisher, event, tracker);
                } catch (CliException e) {
                  if (!continueOnError) {
                    throw new AbortStreamException(e);
                  }
                }
              });
        }
      } catch (AbortStreamException e) {
        return prepareResult();
      } catch (Exception e) {
        tracker.recordFailure("-", "-", e.getMessage());
        System.err.println(e.getMessage());

        if (!continueOnError) {
          return prepareResult();
        }
      }
    } catch (StreamxClientException e) {
      throw new CliException(msg.unableToCreateStreamxClient(ingestionClientConfig.url()), e);
    }

    return prepareResult();
  }

  private void sendEvent(
      Publisher publisher,
      CloudEvent event,
      StreamPublishingTracker tracker
  ) {
    int eventNumber = tracker.nextEventNumber();
    try {
      publisher.send(List.of(event));
      tracker.recordSuccess();

      System.out.println(msg.eventPublished(
          String.valueOf(eventNumber),
          event.getType(),
          event.getSubject()
      ));
    } catch (StreamxClientException e) {
      tracker.recordFailure(
          event.getType(),
          event.getSubject(),
          e.getMessage()
      );

      String errorMessage = msg.eventPublishFailed(
          String.valueOf(eventNumber),
          event.getType(), event.getSubject(), e.getMessage()
      );

      System.err.println(errorMessage);

      throw new CliException(errorMessage);
    }
  }

  @Override
  public String getTextOutput(CommandResult<StreamCommandResult> result) {
    return tracker.toSummary();
  }

}