package com.streamx.cli.commands.publish.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.ingestion.CloudEvents;
import com.streamx.cli.ingestion.ConcatenatedJsonParser;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
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
  public URI source;

  // TODO: Should we make chunk size configurable via CLI options?
  private static final int CHUNK_SIZE = 100;

  @Override
  public CommandResult<Void> runCommand() {
    if (this.verbose) {
      System.out.println("Running stream command");
    }

    if (this.verbose) {
      System.out.println("Resolving StreamX client config");
    }

    IngestionClientConfig ingestionClientConfig = ingestionOptions.getIngestionClientConfig();

    if (this.verbose) {
      System.out.println("Initializing StreamX client with config: %s".formatted(ingestionClientConfig));
    }

    System.out.println("!!! A");
    try (StreamxClient streamxClient = StreamxClientFactory.create(ingestionClientConfig)) {
      try {
        System.out.println("!!! B");
        InputStream sourceStream = getSourceStream();

        byte[] bytes = sourceStream.readAllBytes();
        System.out.println("Source stream: " + new String(bytes, StandardCharsets.UTF_8));
        sourceStream = new ByteArrayInputStream(bytes);

        ConcatenatedJsonParser jsonParser = new ConcatenatedJsonParser();
        Publisher publisher = streamxClient.newPublisher();

        try (Stream<JsonNode> jsonStream = jsonParser.parse(sourceStream)) {
          Spliterator<JsonNode> spliterator = jsonStream.spliterator();
          long knownSize = spliterator.getExactSizeIfKnown();
          int counter = 0;
          List<CloudEvent> chunk = new ArrayList<>();

          Stream<CloudEvent> eventStream = StreamSupport.stream(spliterator, false)
              .map(CloudEvents::fromJsonNode);

          try (eventStream) {
            for (var iterator = eventStream.iterator(); iterator.hasNext(); ) {
              CloudEvent event = iterator.next();
              chunk.add(event);
              if (chunk.size() >= CHUNK_SIZE) {
                counter = sendChunk(publisher, chunk, counter, knownSize);
                chunk.clear();
              }
            }

            if (!chunk.isEmpty()) {
              sendChunk(publisher, chunk, counter, knownSize);
            }
          }
        }

        return new CommandResult<>(null);
      } catch (CliException e) {
        throw e;
      } catch (Exception e) {
        throw new CliException("Unable to stream: " + e.getMessage(), e);
      }
    } catch (StreamxClientException e) {
      throw new CliException("Unable to create StreamX client: " + ingestionClientConfig.url(), e);
    }
  }

  private int sendChunk(
      Publisher publisher,
      List<CloudEvent> chunk,
      int counter,
      long knownSize
  ) {
    if (this.verbose) {
      System.out.printf("Sending chunk of %s events%n", chunk.size());
    }

    for (CloudEvent event : chunk) {
      try {
        publisher.send(List.of(event));
        counter++;
        System.out.printf("Event published (%s): type='%s', source='%s', id='%s'%n",
            formatProgress(counter, knownSize),
            event.getType(), event.getSource(), event.getId());
      } catch (Exception e) {
        counter++;
        System.err.printf("Event publish failed (%s): type='%s', source='%s', id='%s' - %s%n",
            formatProgress(counter, knownSize),
            event.getType(), event.getSource(), event.getId(), e.getMessage());
        throw new CliException("Failed to send event: " + e.getMessage(), e);
      }
    }
    return counter;
  }

  private String formatProgress(int current, long knownSize) {
    if (knownSize >= 0) {
      return current + "/" + knownSize;
    }
    return String.valueOf(current);
  }

  private InputStream getSourceStream() throws CliException {
    InputStream input;
    if (source == null) {
      input = System.in;
    } else {
      try {
        input = source.toURL().openStream();
      } catch (Exception e) {
        throw new CliException("Unable to open source input stream: " + source, e);
      }
    }

    try {
      int firstByte = input.read();
      if (firstByte == -1) {
        throw new CliException("Input is empty.");
      }

      return new SequenceInputStream(
          new ByteArrayInputStream(new byte[]{(byte) firstByte}),
          input
      );
    } catch (Exception e) {
      throw new CliException("Unable to read input stream: " + e.getMessage(), e);
    }
  }
}
