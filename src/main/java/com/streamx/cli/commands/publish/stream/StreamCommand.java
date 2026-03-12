package com.streamx.cli.commands.publish.stream;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.ingestion.CloudEventsSerde;
import com.streamx.cli.ingestion.ConcatenatedJsonSerde;
import com.streamx.cli.ingestion.IngestionClientConfig;
import com.streamx.cli.ingestion.IngestionClientPicocliOptions;
import com.streamx.cli.ingestion.SourceValidator;
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
    description = "Stream a bunch of events")
public class StreamCommand extends AbstractSilentCommand {
  @CommandLine.Mixin
  IngestionClientPicocliOptions ingestionOptions;

  @CommandLine.Parameters(
      index = "0",
      description = "Events source URI",
      arity = "0..1",
      defaultValue = CommandLine.Parameters.NULL_VALUE
  )
  public String source;

  // TODO: Should we make chunk size configurable via CLI options?
  private static final int CHUNK_SIZE = 100;

  @Override
  public CommandResult<Void> runCommand() {
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

        byte[] bytes = sourceStream.readAllBytes();
        sourceStream = new ByteArrayInputStream(bytes);

        Publisher publisher = streamxClient.newPublisher();

        try (Stream<JsonNode> jsonStream = ConcatenatedJsonSerde.parse(sourceStream)) {
          List<CloudEvent> allEvents = jsonStream
              .map(CloudEventsSerde::fromJson)
              .toList();

          int counter = 0;
          List<CloudEvent> chunk = new ArrayList<>();

          for (CloudEvent event : allEvents) {
            chunk.add(event);
            if (chunk.size() >= CHUNK_SIZE) {
              counter = sendChunk(publisher, chunk, counter);
              chunk.clear();
            }
          }

          if (!chunk.isEmpty()) {
            counter = sendChunk(publisher, chunk, counter);
          }

          System.out.println(msg.eventsPublished(counter));
        }

        return new CommandResult<>(null);
      } catch (CliException e) {
        throw e;
      } catch (Exception e) {
        throw new CliException(msg.unableToStream(e.getMessage()), e);
      }
    } catch (StreamxClientException e) {
      throw new CliException(msg.unableToCreateStreamxClient(ingestionClientConfig.url()), e);
    }
  }

  private int sendChunk(
      Publisher publisher,
      List<CloudEvent> chunk,
      int counter
  ) {
    if (this.verbose) {
      System.out.println(msg.sendingChunk(chunk.size()));
    }

    for (CloudEvent event : chunk) {
      try {
        publisher.send(List.of(event));
        counter++;
        System.out.println(msg.eventPublished(
            String.valueOf(counter),
            event.getType(), event.getSource().toString(), event.getId()));
      } catch (Exception e) {
        counter++;
        System.err.println(msg.eventPublishFailed(
            String.valueOf(counter),
            event.getType(), event.getSource().toString(), event.getId(), e.getMessage()));
        throw new CliException(msg.failedToSendEvent(e.getMessage()), e);
      }
    }
    return counter;
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