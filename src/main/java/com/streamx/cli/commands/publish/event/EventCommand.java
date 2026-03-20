package com.streamx.cli.commands.publish.event;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.streamx.cli.commands.publish.EventTemplateProcessor;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.ingestion.CloudEventsSerde;
import com.streamx.cli.ingestion.IngestionClientConfig;
import com.streamx.cli.ingestion.IngestionClientPicocliOptions;
import com.streamx.cli.ingestion.StreamxClientFactory;
import com.streamx.clients.ingestion.StreamxClient;
import com.streamx.clients.ingestion.exceptions.StreamxClientException;
import com.streamx.clients.ingestion.publisher.Publisher;
import io.cloudevents.CloudEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "event",
    mixinStandardHelpOptions = true,
    header = "Publish a single event")
public class EventCommand extends AbstractCommand<EventCommandResult> {

  private static final String RESULT_FILE_NAME = "publish-event-result.json";
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  @CommandLine.Mixin
  IngestionClientPicocliOptions ingestionOptions;

  @CommandLine.Parameters(
      index = "0",
      description = "Event template type",
      arity = "0..1"
  )
  public String eventType;

  @CommandLine.Parameters(
      index = "1",
      description = "Payload path",
      arity = "0..1"
  )
  public String eventPayloadPath;

  @CommandLine.Parameters(
      index = "2",
      description = "Event subject",
      arity = "0..1"
  )
  public String eventSubject;

  @Override
  public String getTextOutput(CommandResult<EventCommandResult> result) {
    return msg.publishEventSucceed(result.getData().subject(), result.getData().templatePath());
  }

  @Override
  public CommandResult<EventCommandResult> runCommand() {
    PayloadPathValidator.validate(eventPayloadPath);

    if (this.verbose) {
      System.err.println(msg.runningPublishEventCommand());
      System.err.println(msg.resolvingStreamxClientConfig());
    }

    IngestionClientConfig ingestionClientConfig = ingestionOptions.getIngestionClientConfig();

    if (this.verbose) {
      System.err.println(msg.initializingStreamxClient());
      System.err.println(IngestionClientConfig.prettyPrint(ingestionClientConfig));
    }

    EventTemplateLoader templateLoader = new EventTemplateLoader();
    EventTemplateLoader.TemplateDescriptor templateDescriptor = templateLoader.load(eventType);
    Path payloadPath = Path.of(eventPayloadPath);

    EventTemplateProcessor templateProcessor =
        new EventTemplateProcessor(templateDescriptor.template(), payloadPath, eventSubject);
    CloudEvent cloudEvent = templateProcessor.toCloudEvent();

    try (StreamxClient streamxClient = StreamxClientFactory.create(ingestionClientConfig)) {
      try {
        EventCommandResult result = new EventCommandResult(
            null,
            cloudEvent.getSubject(),
            templateDescriptor.templatePath(),
            templateDescriptor.template(),
            CloudEventsSerde.toJson(cloudEvent)
        );

        Publisher publisher = streamxClient.newPublisher();
        publisher.send(cloudEvent);

        return new CommandResult<>(result);
      } catch (Exception e) {
        EventCommandResult errorDetails = new EventCommandResult(
            e.getMessage(),
            cloudEvent.getSubject(),
            templateDescriptor.templatePath(),
            templateDescriptor.template(),
            CloudEventsSerde.toJson(cloudEvent)
        );

        String errorDetailsPath = writeErrorDetailsToTempFile(errorDetails);

        throw new CliException(msg.publishEventFailed(e.getMessage(), errorDetailsPath), e);
      }
    } catch (StreamxClientException e) {
      throw new CliException(msg.unableToCreateStreamxClient(ingestionClientConfig.url()), e);
    }
  }

  private String writeErrorDetailsToTempFile(EventCommandResult result) {
    try {
      String prefix = "streamx-cli-" + LocalDate.now().format(DATE_FORMAT) + "-";
      Path tempDir = Files.createTempDirectory(prefix);
      Path resultFile = tempDir.resolve(RESULT_FILE_NAME);

      ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
      mapper.writeValue(resultFile.toFile(), result);

      return resultFile.toAbsolutePath().toString();
    } catch (IOException e) {
      throw new CliException(msg.failedToSavePublishEventErrorDetails(e.getMessage()), e);
    }
  }
}