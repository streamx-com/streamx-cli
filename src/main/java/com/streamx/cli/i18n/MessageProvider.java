package com.streamx.cli.i18n;

import com.streamx.runner.config.StreamxBaseConfig;
import java.lang.invoke.MethodHandles;
import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = "")
public interface MessageProvider {

  MessageProvider msg = Messages.getBundle(MethodHandles.lookup(), MessageProvider.class);

  @Message(id = 100, value = "Unsupported output format")
  String unsupportedOutputFormat();

  @Message(id = 101, value = "Try '%s%s' for more information on the available options%n")
  String tryForMoreInformationOnAvailableOptions(
      String qualifiedCommandName,
      String helpOptionName
  );

  @Message(id = 102, value = "Failed to handle interactive input")
  String failedToHandleInteractiveInput();

  @Message(id = 103, value = "Unable to serialize JSON")
  String unableToSerializeJson();

  @Message(
      id = 104,
      value = """
          ❌ Something went wrong while running the command.
          
          Please try again with `--verbose` for more details.
          If the problem persists, report it here:
          https://www.streamx.dev/contact-us.html
          """
  )
  String somethingWentWrong();

  @Message(
      id = 110,
      value = """
        Timeout exceeded waiting for the container "%s" after %d seconds.
                
        Try increasing the timeout by setting the """
          + StreamxBaseConfig.PN_CONTAINER_STARTUP_TIMEOUT_SECONDS + " property"
  )
  String dockerContainerStartupFailed(String containerName, Long timeoutSecs);

  @Message(id = 111, value = "Failed to start mesh containers. %s")
  String failedToStartMeshContainers(String reason);

  @Message(id = 112, value = """
        Could not find a valid Docker environment.

        Make sure that:
         * Docker is installed,
         * Docker is running""")
  String invalidDockerEnvironment();

  @Message(id = 113, value = "🟢 %s ready")
  String dockerContainerStarted(String containerName);

  @Message(id = 114, value = "🟢 %s stopped")
  String dockerContainerStopped(String containerName);

  @Message(id = 115, value = "❌ %s failed")
  String dockerContainerFailed(String containerName);

  @Message(id = 116, value = "Mesh file deleted. Stopping...")
  String meshFileDeleted();

  @Message(id = 117, value = "Mesh stopped")
  String meshStopped();

  @Message(id = 118, value = "Unknown action: %s. Skipping...")
  String skippingUnknownAction(String action);

  @Message(id = 119, value = "Failed to watch mesh changes")
  String failedToWatchMeshChanges();

  @Message(id = 120, value = "Setting up system containers...")
  String settingUpSystemContainers();

  @Message(id = 121, value = "Starting mesh...")
  String startingMesh();

  @Message(id = 122, value = "Stopping mesh...")
  String stoppingMesh();

  @Message(id = 123, value = """
      Unable to read mesh definition from %s
      
      Details:
      %s""")
  String unableToReadMeshDefinition(String fromPath, String details);

  @Message(id = 124, value = "Mesh definition is invalid. Skip reloading...")
  String meshDefinitionIsInvalidSkipReload();

  @Message(id = 125, value = "Mesh definition is unchanged. Skip reloading...")
  String meshDefinitionIsUnchangedSkipReload();

  @Message(id = 126, value = "Mesh file changed. Processing full reload...")
  String meshFileChangedFullReload();

  @Message(id = 127, value = "Mesh file changed. Processing incremental reload...")
  String meshFileChangedIncrementalReload();

  @Message(id = 128, value = "Mesh reloaded")
  String meshReloaded();

  @Message(id = 129, value = "Mesh reload failed")
  String meshReloadFailed();

  @Message(id = 130, value = """
      %s
        
      Full logs can be found in %s""")
  String fullLogsCanBeFoundIn(String originalMessage, String logPath);

  @Message(id = 131, value = "Execution exception occurred")
  String executionExceptionOccurred();

  @Message(id = 132, value = "Input path must not be null")
  String inputPathMustNotBeNull();

  @Message(id = 133, value = "Path %s does not have %s parent levels")
  String pathDoesNotHaveParentLevels(String path, int parentLevelsCount);

  @Message(id = 134, value = "Failed to initialize streamx-maven properties")
  String failedToInitializeStreamxMavenProperties();

  @Message(id = 135, value = "No version information included")
  String noVersionInformationIncluded();

  @Message(id = 136, value = "Expression cannot be null")
  String expressionCannotBeNull();

  @Message(id = 137, value = "Could not expand value %s in expression %s")
  String couldNotExpandValueInExpression(String key, String expression);

  @Message(id = 138, value = "Mesh file not found at: %s")
  String meshFileNotFound(String path);

  @Message(id = 139, value = """
      StreamX settings properties:
      =================================
      """)
  String listSettingsHeader();

  @Message(id = 140, value = "No StreamX settings properties found")
  String listSettingsNoPropertiesFound();

  @Message(id = 105, value = "No such settings property found: %s")
  String noSettingsPropertyFound(String key);

  @Message(id = 106, value = "Unable to get settings property")
  String unableToGetSettingsProperty();

  @Message(id = 107, value = "Failed to load properties from: %s")
  String failedToLoadPropertiesFrom(String path);

  @Message(id = 108, value = "Unable to set settings property")
  String unableToSetSettingsProperty();

  @Message(id = 109, value = "Unable to get settings file path")
  String unableToGetSettingsFilePath();

  @Message(id = 141, value = "Running publish stream command")
  String runningPublishStreamCommand();

  @Message(id = 142, value = "Resolving StreamX client config")
  String resolvingStreamxClientConfig();

  @Message(id = 143, value = "Initializing StreamX client with config:")
  String initializingStreamxClient();

  @Message(id = 144, value = "Sending chunk of %s events")
  String sendingChunk(int size);

  @Message(id = 145, value = "Event published (%s): type='%s', subject='%s'")
  String eventPublished(String progress, String type, String subject);

  @Message(id = 146, value = "Event publish failed (%s): type='%s', subject='%s' - %s")
  String eventPublishFailed(String progress, String type, String subject, String error);

  @Message(id = 147, value = "Failed to send event: %s")
  String failedToSendEvent(String reason);

  @Message(id = 148, value = "Unable to publish stream: %s")
  String unableToPublishStream(String reason);

  @Message(id = 149, value = "Unable to create StreamX client: %s")
  String unableToCreateStreamxClient(String url);

  @Message(id = 150, value = "Paste JSON content below. Press Ctrl+D when done:")
  String pasteJsonContent();

  @Message(id = 151, value = "Input is empty")
  String inputIsEmpty();

  @Message(id = 152, value = "Unable to open source input stream: %s - %s")
  String unableToOpenSourceInputStream(String source, String reason);

  @Message(id = 153, value = "Unable to read input stream: %s")
  String unableToReadInputStream(String reason);

  @Message(id = 154, value = "Connection refused")
  String connectionRefused();

  @Message(id = 155, value = "Invalid source URI: '%s'")
  String invalidSourceUri(String source);

  @Message(id = 156, value = "Source file not found: '%s'")
  String sourceFileNotFound(String path);

  @Message(id = 157, value = "Source file is not readable: '%s'")
  String sourceFileNotReadable(String path);

  @Message(id = 158, value = "Publishing stream from directory is not supported. Path: '%s'")
  String sourceIsDirectory(String path);

  @Message(id = 162, value = "CloudEvent deserialization failed: %s")
  String cloudEventDeserializationFailed(String reason);

  @Message(id = 163, value = "CloudEvent serialization failed: %s")
  String cloudEventSerializationFailed(String reason);

  @Message(id = 164, value = "Failed to parse JSON: %s")
  String failedToParseJson(String reason);

  @Message(id = 165, value = "Failed to close JSON parser: %s")
  String failedToCloseJsonParser(String reason);

  @Message(id = 166, value = "Failed to serialize JSON sequence: %s")
  String failedToSerializeJsonSequence(String reason);

  @Message(id = 167, value = "<not set>")
  String ingestionTokenNotSet();

  @Message(id = 168, value = "*****")
  String ingestionTokenMasked();

  @Message(id = 169, value = """
        Stream publishing completed
          Total events:  %d
          Successful:    %d
          Failed:        %d
          Unknown:    %d""")
  String streamPublishingCompleted(int total, int successful, int failed, int unknown);

  @Message(id = 173, value = "First %d error(s) are shown:")
  String streamFirstErrors(int count);

  @Message(id = 174, value = "  Event #%d [type=%s, subject=%s]: %s")
  String streamEventError(
      int eventNumber,
      String type,
      String subject,
      String errorMessage
  );

  @Message(id = 175, value = "  ... and %d more error(s) not shown")
  String streamMoreErrorsNotShown(int count);

  @Message(id = 176, value = "One or more events failed to publish")
  String eventsPartiallyFailedToPublish();

  @Message(id = 177, value = "Batch #%s published (%s event(s))")
  String batchPublished(String batchNumber, String eventCount);

  @Message(id = 178, value = "Batch #%s failed (%s event(s)): %s")
  String batchPublishFailed(String batchNumber, String eventCount, String errorMessage);

  @Message(id = 179, value = """
      Stream publishing completed
        Total events:          %d
        Successful:            %d
        Failed:                %d
        Unknown:               %d
        Total batches:         %d
        Successful batches:    %d
        Failed batches:        %d""")
  String streamBatchPublishingCompleted(
      int totalEvents,
      int successCount,
      int failureCount,
      int unknownCount,
      int totalBatches,
      int batchSuccessCount,
      int batchFailureCount
  );

  @Message(id = 180, value = "First %d batch(es) publish errors are shown:")
  String streamFirstBatchErrors(int count);

  @Message(id = 181, value = "  Batch #%d (%d event(s)): %s")
  String streamBatchError(int batchNumber, int eventCount, String errorMessage);

  @Message(id = 182, value = "Event publish result is unknown. Failed batch number: %s")
  String eventPublishResultIsUnknown(int batchNumber);
}
