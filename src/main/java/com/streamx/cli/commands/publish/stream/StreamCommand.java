package com.streamx.cli.commands.publish.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import com.streamx.cli.ingestion.CloudEvents;
import com.streamx.cli.ingestion.ConcatenatedJsonParser;
import com.streamx.cli.ingestion.IngestionClientConfig;
import com.streamx.cli.ingestion.IngestionClientPicocliOptions;
import com.streamx.cli.ingestion.StreamxClientFactory;
import com.streamx.clients.ingestion.StreamxClient;
import com.streamx.clients.ingestion.exceptions.StreamxClientException;
import com.streamx.clients.ingestion.publisher.Publisher;
import io.cloudevents.CloudEvent;
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
      description = "Path to mesh definition file.",
      arity = "0..1",
      defaultValue = CommandLine.Parameters.NULL_VALUE
  )
  public URI source;

  private static final int CHUNK_SIZE = 100;

  @Override
  public CommandResult<Void> runCommand() {
    IngestionClientConfig ingestionClientConfig = ingestionOptions.getIngestionClientConfig();

    try (StreamxClient streamxClient = StreamxClientFactory.create(ingestionClientConfig)) {
      try {
        InputStream sourceStream = getSourceStream();
        ConcatenatedJsonParser jsonParser = new ConcatenatedJsonParser();
        Publisher publisher = streamxClient.newPublisher();

        try (Stream<JsonNode> jsonStream = jsonParser.parse(sourceStream)) {
          List<CloudEvent> chunk = new ArrayList<>();

          jsonStream
              .map(CloudEvents::fromJsonNode)
              .forEach(event -> {
                chunk.add(event);
                if (chunk.size() >= CHUNK_SIZE) {
                  sendChunk(publisher, chunk);
                  chunk.clear();
                }
              });

          if (!chunk.isEmpty()) {
            sendChunk(publisher, chunk);
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

  private void sendChunk(Publisher publisher, List<CloudEvent> chunk) {
    try {
      publisher.send(new ArrayList<>(chunk));
    } catch (Exception e) {
      throw new CliException("Failed to send events: " + e.getMessage(), e);
    }
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
