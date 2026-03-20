package com.streamx.cli.commands.publish.events;

import static com.streamx.cli.commands.publish.events.TemplateLoader.EVENTTEMPLATE_FILE;
import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.commands.publish.AbortStreamException;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.ingestion.IngestionClientConfig;
import com.streamx.cli.ingestion.IngestionClientPicocliOptions;
import com.streamx.cli.ingestion.StreamxClientFactory;
import com.streamx.clients.ingestion.StreamxClient;
import com.streamx.clients.ingestion.exceptions.StreamxClientException;
import com.streamx.clients.ingestion.publisher.Publisher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "events",
    mixinStandardHelpOptions = true,
    header =
        "Publishes multiple events based on a directory structure and .eventtemplate files")
public class EventsCommand extends AbstractCommand<EventsCommandResult> {

  @CommandLine.Mixin
  IngestionClientPicocliOptions ingestionOptions;

  @CommandLine.Parameters(
      index = "0",
      description = "Path to directory containing a .eventtemplate file (absolute or relative)"
  )
  public String path;

  @CommandLine.Option(
      names = {"--patch", "-p"},
      description = "Name of the patch to apply (.{patchName}.eventtemplate)"
  )
  public String patchName;

  @CommandLine.Option(
      names = {"--continue-on-error", "-x"},
      description = "Continue even if some event publish failed"
  )
  public boolean continueOnError;

  @CommandLine.Option(
      names = {"--batch-size", "-b"},
      description = "Publish events in batches if > 1. Per-event error reporting is omitted",
      defaultValue = "1"
  )
  public Integer batchSize;

  @CommandLine.Option(
      names = {"--dry-run"},
      description =
          "Render all events (applying templates and patches) and write the results to a "
              + "temporary directory for inspection, without publishing anything to StreamX"
  )
  public boolean dryRun;

  @CommandLine.Option(
      names = {"--debug"},
      description =
          "Publish events normally AND write the rendered artefacts (templates, patched "
              + "templates, resolved events) to a temporary directory for inspection"
  )
  public boolean debug;

  PublishingTracker tracker = new PublishingTracker();

  @Override
  public CommandResult<EventsCommandResult> runCommand() {
    Path rootPath = Paths.get(path).toAbsolutePath().normalize();

    Path rootTemplateFile = rootPath.resolve(EVENTTEMPLATE_FILE);
    if (!Files.exists(rootTemplateFile)) {
      throw new CliException(msg.noEventTemplateInsideDirectory(rootPath.toString()));
    }

    if (this.verbose) {
      System.err.println(msg.resolvingStreamxClientConfig());
    }

    IngestionClientConfig ingestionClientConfig = ingestionOptions.getIngestionClientConfig();

    if (this.verbose) {
      System.err.println(msg.initializingStreamxClient());
      System.err.println(IngestionClientConfig.prettyPrint(ingestionClientConfig));
    }

    JsonNode rootTemplate = TemplateLoader.load(rootTemplateFile, rootPath);
    String rootTemplatePath = rootTemplateFile.toString();
    String appliedPatch = null;
    String patchPath = null;

    if (patchName != null) {
      rootTemplate = TemplateLoader.applyPatch(
          rootPath, rootTemplate, patchName,
          () -> confirmContinueWithoutPatch(rootPath, patchName));
      if (rootTemplate == null) {
        return prepareResult();
      }
      appliedPatch = patchName;

      Path patchFile = rootPath.resolve("." + patchName + EVENTTEMPLATE_FILE);
      patchPath = Files.exists(patchFile) ? patchFile.toString() : null;
    }

    TemplateContext rootContext =
        new TemplateContext(rootTemplate, rootTemplatePath, appliedPatch, patchPath);

    DebugDirectoryWriter debugDirectoryWriter = null;
    if (dryRun || debug) {
      try {
        debugDirectoryWriter = new DebugDirectoryWriter(rootPath);
        if (dryRun && debug) {
          System.err.println(
              msg.dryRunAndDebugSpecified(debugDirectoryWriter.getTempDir().toString())
          );
        } else if (dryRun) {
          System.err.println(msg.dryRunMode(debugDirectoryWriter.getTempDir().toString()));
        } else {
          System.err.println(msg.debugMode(debugDirectoryWriter.getTempDir().toString()));
        }
      } catch (IOException e) {
        throw new CliException(msg.failedToCreateOutputDirectory(e.getMessage()), e);
      }
    }

    try (StreamxClient streamxClient = StreamxClientFactory.create(ingestionClientConfig)) {
      Publisher publisher = streamxClient.newPublisher();
      DirectoryWalker walker = new DirectoryWalker(
          tracker,
          publisher,
          batchSize,
          continueOnError,
          debugDirectoryWriter,
          dryRun
      );

      try {
        walker.walk(rootPath, rootContext);
        walker.flushBatch();
      } catch (AbortStreamException e) {
        return prepareResult();
      } catch (Exception e) {
        tracker.recordFailure("''", "''", rootTemplatePath, appliedPatch, e.getMessage());
        System.err.println(e.getMessage());
        if (!continueOnError) {
          return prepareResult();
        }
      }
    } catch (StreamxClientException e) {
      throw new CliException(msg.unableToCreateStreamxClient(ingestionClientConfig.url()), e);
    }

    if (debugDirectoryWriter != null) {
      System.err.println(
          msg.inspectRenderedEventsIn(debugDirectoryWriter.getTempDir().toString())
      );
    }

    return prepareResult();
  }

  private boolean confirmContinueWithoutPatch(Path rootPath, String patchName) {
    String response = promptForInput(
        msg.patchNotFound(patchName, rootPath.toString()), List.of("y", "n"));
    return "y".equalsIgnoreCase(response);
  }

  private CommandResult<EventsCommandResult> prepareResult() {
    EventsCommandResult eventsResult = tracker.toResult();
    CommandResult<EventsCommandResult> result = new CommandResult<>(eventsResult);

    boolean someEventsFailed = !eventsResult.eventErrors().isEmpty()
        || !eventsResult.batchErrors().isEmpty();

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
  public String getTextOutput(CommandResult<EventsCommandResult> result) {
    return tracker.toSummary();
  }
}