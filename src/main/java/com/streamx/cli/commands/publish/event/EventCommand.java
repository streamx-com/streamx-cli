package com.streamx.cli.commands.publish.event;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.ingestion.*;
import com.streamx.cli.ingestion.SourceValidator;
import com.streamx.clients.ingestion.StreamxClient;
import com.streamx.clients.ingestion.exceptions.StreamxClientException;
import com.streamx.clients.ingestion.publisher.Publisher;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import static com.streamx.cli.i18n.MessageProvider.msg;

@Command(name = "event",
    mixinStandardHelpOptions = true,
    description = "Publish a single event")
public class EventCommand extends AbstractSilentCommand {
  @CommandLine.Mixin
  IngestionClientPicocliOptions ingestionOptions;

  @CommandLine.Parameters(
      index = "0",
      description = "Template type",
      arity = "0..1"
  )
  public String eventType;

  @CommandLine.Parameters(
      index = "0",
      description = "Template type",
      arity = "0..1"
  )
  public String eventPayloadPath;

  @CommandLine.Parameters(
      index = "0",
      description = "Event subject",
      arity = "0..1"
  )
  public String eventSubject;

  @Override
  public CommandResult<Void> runCommand() {
    SourceValidator.validate(eventPayloadPath);

    if (this.verbose) {
      System.out.println(msg.runningPublishEventCommand());
      System.out.println(msg.resolvingStreamxClientConfig());
    }

    IngestionClientConfig ingestionClientConfig = ingestionOptions.getIngestionClientConfig();

    if (this.verbose) {
      System.out.println(msg.initializingStreamxClient());
      System.out.println(IngestionClientConfig.prettyPrint(ingestionClientConfig));
    }

    String eventTemplateString = TemplateResolver.getEventTemplate(eventType);

    try (StreamxClient streamxClient = StreamxClientFactory.create(ingestionClientConfig)) {
      try {
//        InputStream sourceStream = getSourceStream();

//        byte[] bytes = sourceStream.readAllBytes();
//        sourceStream = new ByteArrayInputStream(bytes);

        Publisher publisher = streamxClient.newPublisher();

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
}