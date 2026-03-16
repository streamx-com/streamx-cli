package com.streamx.cli.commands.publish.event;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.publish.stream.SourceValidator;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.ingestion.IngestionClientConfig;
import com.streamx.cli.ingestion.IngestionClientPicocliOptions;
import com.streamx.cli.ingestion.StreamxClientFactory;
import com.streamx.clients.ingestion.StreamxClient;
import com.streamx.clients.ingestion.exceptions.StreamxClientException;
import com.streamx.clients.ingestion.publisher.Publisher;
import io.cloudevents.CloudEvent;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.nio.file.Path;

@Command(name = "event",
    mixinStandardHelpOptions = true,
    description = "Publish a single event")
public class EventCommand extends AbstractSilentCommand {
  @CommandLine.Mixin
  IngestionClientPicocliOptions ingestionOptions;

  @CommandLine.Parameters(
      index = "0",
      description = "Event template type",
      arity = "0..1"
  )
  public String eventType;

  @CommandLine.Parameters(
      index = "0",
      description = "Payload path",
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

    EventTemplateLoader templateLoader = new EventTemplateLoader();
    String templateJson = templateLoader.load(eventType);
    Path payloadPath = Path.of(eventPayloadPath);

    EventTemplateProcessor templateProcessor =
        new EventTemplateProcessor(templateJson, payloadPath, eventSubject);
    CloudEvent cloudEvent = templateProcessor.toCloudEvent();

    try (StreamxClient streamxClient = StreamxClientFactory.create(ingestionClientConfig)) {
      try {
        Publisher publisher = streamxClient.newPublisher();
        publisher.send(cloudEvent);

        return new CommandResult<>(null);
      } catch (Exception e) {
        throw new CliException(msg.unableToPublishEvent(e.getMessage()), e);
      }
    } catch (StreamxClientException e) {
      throw new CliException(msg.unableToCreateStreamxClient(ingestionClientConfig.url()), e);
    }
  }
}