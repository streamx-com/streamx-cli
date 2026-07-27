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

  @Message(id = 429, value = "Token '%s' created. Copy it now - it will not be shown again.")
  String authTokenCreated(String name);

  @Message(id = 430, value = "Token revoked")
  String authTokenRevoked();

  @Message(id = 431, value = "No personal access tokens")
  String authTokenListEmpty();

  @Message(id = 432, value = "Could not read the profile the token belongs to")
  String authTokenIdentityUnavailable();

  @Message(id = 433, value = "Not authorized. The personal access token in "
      + "STREAMX_PLATFORM_TOKEN is invalid or has been revoked")
  String platformTokenUnauthorized();

  @Message(id = 434, value = "A personal access token cannot manage personal access tokens. "
      + "Unset %s and run 'streamx auth login' first.")
  String authTokenNeedsLoginSession(String variableName);

  @Message(id = 101, value = "Try '%s%s' for more information on the available options%n")
  String tryForMoreInformationOnAvailableOptions(
      String qualifiedCommandName,
      String helpOptionName
  );

  @Message(id = 102, value = "Failed to handle interactive input")
  String failedToHandleInteractiveInput();

  @Message(id = 103, value = "Unable to serialize JSON: %s")
  String unableToSerializeJson(String reason);

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

  @Message(id = 1220, value = "Error during stopping mesh: %s")
  String errorDuringStoppingMesh(String reason);

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

  @Message(id = 138, value = "Mesh file not found at: %s")
  String meshFileNotFound(String path);

  @Message(id = 140, value = "No StreamX settings properties found")
  String listSettingsNoPropertiesFound();

  @Message(id = 105, value = "No such settings property found: %s")
  String noSettingsPropertyFound(String key);

  @Message(id = 106, value = "Unable to get settings property: %s")
  String unableToGetSettingsProperty(String reason);

  @Message(id = 107, value = "Failed to load properties from: %s")
  String failedToLoadPropertiesFrom(String path);

  @Message(id = 108, value = "Unable to set settings property")
  String unableToSetSettingsProperty();

  @Message(id = 109, value = "Unable to get settings file path")
  String unableToGetSettingsFilePath();

  @Message(id = 1090, value = "Unable to unset settings property %s: %s")
  String unableToUnsetSettingsProperty(String key, String reason);

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

  @Message(id = 183, value = "Running publish event command")
  String runningPublishEventCommand();

  @Message(id = 184, value = "No event template set for: %s")
  String eventTemplateNotFound(String templateId);

  @Message(id = 185, value = "Event template for %s is corrupted")
  String invalidEventTemplate(String templateId);

  @Message(id = 186, value = "Unable to publish event: %s. You can find error details at: %s")
  String publishEventFailed(String reason, String errorDetailsPath);

  @Message(id = 187, value = "Failed to save publish event error details: %s")
  String failedToSavePublishEventErrorDetails(String reason);

  @Message(id = 188, value = "Invalid payload path: %s")
  String invalidPayloadPath(String payloadPath);

  @Message(id = 189, value = "Payload path not found: %s")
  String payloadFileNotFound(String path);

  @Message(id = 190, value = "Payload file is not readable: %s")
  String payloadFileNotReadable(String path);

  @Message(id = 191, value = "Payload should be a file, but directory found. Path: %s")
  String payloadFileIsDirectory(String path);

  @Message(id = 192, value = "Published %s using %s")
  String publishEventSucceed(String eventSubject, String templatePath);

  @Message(id = 193, value = "Failed to process event template placeholders: %s")
  String failedToProcessEventTemplatePlaceholders(String reason);

  @Message(id = 194, value = "No .eventtemplate file inside %s")
  String noEventTemplateInsideDirectory(String path);

  @Message(id = 195, value = ".eventtemplate inside %s is corrupted")
  String eventTemplateCorrupted(String path);

  @Message(
      id = 196,
      value = "%s not found inside %s. No patch will be applied. Do you want to continue y/n"
  )
  String patchNotFound(String patchName, String path);

  @Message(id = 197, value = "Patch %s is invalid")
  String patchIsInvalid(String patchName);

  @Message(id = 198, value = "%s events published")
  String eventsPublished(int count);

  @Message(id = 199, value = "Failed to write debug artefacts for %s: %s")
  String failedToWriteDebugArtefacts(String payloadPath, String reason);

  @Message(id = 200, value = "Inspect rendered events in: %s")
  String inspectRenderedEventsIn(String path);

  @Message(
      id = 201,
      value = "Both --dry-run and --debug specified; --dry-run takes precedence. "
          + "Events will NOT be published. Rendered output will be written to: %s"
  )
  String dryRunAndDebugSpecified(String tempDir);

  @Message(
      id = 202,
      value = "Dry-run mode: events will NOT be published. "
          + "Rendered output will be written to: %s"
  )
  String dryRunMode(String tempDir);

  @Message(
      id = 203,
      value = "Debug mode: events WILL be published. "
          + "Rendered output will be written to: %s"
  )
  String debugMode(String tempDir);

  @Message(id = 204, value = "Failed to create output directory: %s")
  String failedToCreateOutputDirectory(String reason);

  @Message(id = 205, value = "streamx")
  String rootCommandName();

  @Message(id = 206, value = "[...options]")
  String synopsisOptions();

  @Message(id = 207, value = "[COMMAND]")
  String synopsisCommand();

  @Message(id = 208, value = "WARNING: Environment variable '%s'"
      + " used in expression '%s' is not set")
  String unresolvedEnvironmentVariable(String key, String expression);

  @Message(id = 209, value = "Property '%s' used in expression '%s' is not set")
  String unresolvedProperty(String key, String expression);

  @Message(id = 210, value = "Error details saved to: %s")
  String errorDetailsSavedTo(String path);

  @Message(id = 211, value = "Default event templates index resource not found at %s")
  String defaultEventTemplatesIndexNotFound(String resourcePath);

  @Message(id = 212, value = "Unable to read default event templates index from %s")
  String unableToReadDefaultEventTemplatesIndex(String resourcePath);

  @Message(id = 213, value = "No event templates found.")
  String eventTemplatesNoTemplatesFound();

  @Message(id = 215, value = "Failed to list event templates from %s: %s")
  String failedToListEventTemplates(String path, String reason);

  @Message(id = 216, value = "Template ID")
  String eventTemplateCreatePromptId();

  @Message(id = 217, value = "CloudEvent type")
  String eventTemplateCreatePromptType();

  @Message(id = 218, value = "Template ID is required")
  String eventTemplateIdRequired();

  @Message(id = 219, value = "An event template with ID '%s' already exists at %s")
  String eventTemplateAlreadyExists(String id, String path);

  @Message(id = 233, value = "Please pick a different template ID.")
  String eventTemplatePickDifferentId();

  @Message(id = 234,
      value = "This will delete %s and restore the bundled default templates. Proceed? [y/N]")
  String eventTemplatesResetConfirm(String path);

  @Message(id = 235, value = "Reset cancelled.")
  String eventTemplatesResetCancelled();

  @Message(id = 236, value = "Failed to delete %s: %s")
  String eventTemplatesResetDeleteFailed(String path, String reason);

  @Message(id = 237, value = "Failed to repopulate default event templates at %s")
  String eventTemplatesResetFailed(String path);

  @Message(id = 238, value = "Reset default event templates at %s (%d restored)")
  String eventTemplatesResetSucceeded(String path, int count);

  @Message(id = 220, value = "Failed to create event template at %s: %s")
  String failedToCreateEventTemplate(String path, String reason);

  @Message(id = 221, value = "Created event template '%s' at %s")
  String eventTemplateCreated(String id, String path);

  @Message(id = 222, value = "No event templates are registered in settings")
  String eventTemplateNoSettingsRegistrations();

  @Message(id = 223, value = "Pick an event template to unregister")
  String eventTemplateUnregisterPrompt();

  @Message(id = 224, value = "Event template '%s' is not registered in settings")
  String eventTemplateNotRegisteredInSettings(String id);

  @Message(id = 225, value = "Pick an event template to show")
  String eventTemplateGetPrompt();

  @Message(id = 226, value = "Pick an event template to edit")
  String eventTemplateEditPrompt();

  @Message(id = 227, value = "Event template file is missing on disk: %s")
  String eventTemplateFileMissing(String path);

  @Message(id = 228, value = "Failed to copy default template from %s to %s: %s")
  String failedToCopyEventTemplate(String from, String to, String reason);

  @Message(id = 229, value = "Failed to launch editor '%s': %s")
  String failedToLaunchEditor(String editor, String reason);

  @Message(id = 230, value = "Editor '%s' exited with non-zero status %d")
  String editorExitedWithError(String editor, int exitCode);

  @Message(id = 231, value = "Edited event template '%s' at %s")
  String eventTemplateEdited(String id, String path);

  @Message(id = 240, value = "Failed to delete %s: %s")
  String pathDeleteFailed(String path, String reason);

  @Message(id = 241, value = "Saved template at %s is not valid JSON: %s")
  String eventTemplateEditInvalidJson(String path, String reason);

  @Message(id = 242, value = "Re-opening in %s so you can fix the error...")
  String eventTemplateEditReopening(String editor);

  @Message(id = 264, value = "ERROR: invalid JSON. Fix the file below "
      + "and delete every line starting with `//` before saving "
      + "(JSON does not support comments).")
  String eventTemplateEditErrorBannerHeader();

  @Message(id = 270,
      value = "Absolute path of the event payload file passed to `publish event`.")
  String placeholderDescriptionPayloadPath();

  @Message(id = 271,
      value = "Base64-encoded content of the payload file. "
          + "Use this to embed binary or arbitrary text payloads inside a JSON event.")
  String placeholderDescriptionPayloadContentBase64();

  @Message(id = 272,
      value = "Content of the payload file parsed as JSON and inlined directly into "
          + "the event (the surrounding string node is replaced by the parsed JSON).")
  String placeholderDescriptionPayloadContentJson();

  @Message(id = 273,
      value = "Path of the payload file relative to the event template's location. "
          + "Supports the syntax ${relativePath:n}, where 'n' specifies how many "
          + "additional parent directories above the template to include. "
          + "Example: ${relativePath:0} resolves to the path relative to the template "
          + "directory; ${relativePath:1} adds one extra parent level.")
  String placeholderDescriptionRelativePath();

  @Message(id = 274,
      value = "Subject of the event. Resolves to the value passed as the third positional "
          + "argument of `publish event`, or to ${payloadPath} when no subject is given.")
  String placeholderDescriptionSubject();

  @Message(id = 275,
      value = "Universally unique identifier (UUID v4), regenerated for every published event.")
  String placeholderDescriptionUuid();

  @Message(id = 276,
      value = "Current timestamp at the moment of publishing, in ISO_OFFSET_DATE_TIME format.")
  String placeholderDescriptionCurrentTime();

  @Message(id = 243, value = "Cannot delete a default template. "
      + "Use `streamx settings event-templates reset-default-templates` to restore defaults.")
  String eventTemplateCannotDeleteDefault();

  @Message(id = 244, value = "Cannot delete a registered template. "
      + "Use `streamx settings event-templates unregister %s` instead.")
  String eventTemplateCannotDeleteRegistered(String id);

  @Message(id = 245, value = "Delete event template '%s' at %s? [y/N]")
  String eventTemplateDeleteConfirm(String id, String path);

  @Message(id = 246, value = "Delete cancelled.")
  String eventTemplateDeleteCancelled();

  @Message(id = 247, value = "Deleted event template '%s'")
  String eventTemplateDeleted(String id);

  @Message(id = 248, value = "Pick an event template to delete")
  String eventTemplateDeletePrompt();

  @Message(id = 249, value = "Source template ID")
  String eventTemplateCopySourcePrompt();

  @Message(id = 250, value = "New template ID")
  String eventTemplateCopyDestPrompt();

  @Message(id = 251, value = "Copied '%s' to '%s' at %s")
  String eventTemplateCopied(String sourceId, String destId, String path);

  @Message(id = 252, value = "Failed to copy '%s' to %s: %s")
  String failedToCopyEventTemplateTo(String sourceId, String targetPath, String reason);

  @Message(id = 253, value = "Template '%s' is valid")
  String eventTemplateValidOk(String id);

  @Message(id = 254, value = "Template '%s' is invalid: %s")
  String eventTemplateValidFailed(String id, String reason);

  @Message(id = 255, value = "Missing required CloudEvents field: '%s'")
  String eventTemplateMissingCloudEventField(String field);

  @Message(id = 256, value = "Pick an event template to validate")
  String eventTemplateValidatePrompt();

  @Message(id = 257, value = "Pick an event template to rename")
  String eventTemplateRenamePrompt();

  @Message(id = 258, value = "New template ID")
  String eventTemplateRenameDestPrompt();

  @Message(id = 259, value = "Renamed '%s' to '%s'")
  String eventTemplateRenamed(String oldId, String newId);

  @Message(id = 260, value = "Cannot rename a default template. "
      + "Use `copy` to clone it under a new ID instead.")
  String eventTemplateCannotRenameDefault();

  @Message(id = 261, value = "Pick an event template to look up")
  String eventTemplateWhichPrompt();

  @Message(id = 262, value = "CloudEvent type is required")
  String eventTemplateTypeRequired();

  @Message(id = 263, value = "(TAB for options)")
  String interactivePickerHint();

  @Message(
      id = 277,
      value = "StreamX auth server URL is not configured.%n"
          + "Set it with: streamx settings set %s <url>"
  )
  String authServerUrlNotConfigured(String key);

  @Message(id = 278, value = "%s does not support the device authorization flow")
  String authDeviceFlowUnsupported(String issuerUrl);

  @Message(
      id = 279,
      value = "To finish signing in, open:%n  %s%nand enter the code:%n  %s%n%nWaiting..."
  )
  String authLoginInstructions(String verificationUri, String userCode);

  @Message(id = 280, value = "Or open this link directly:%n  %s")
  String authLoginDirectLink(String verificationUriComplete);

  @Message(id = 281, value = "Logged in successfully")
  String authLoginSuccess();

  @Message(id = 282, value = "Login was denied")
  String authLoginDenied();

  @Message(id = 283, value = "Login timed out before it was confirmed. Run 'streamx auth login'"
      + " again")
  String authLoginExpired();

  @Message(id = 284, value = "Login failed: %s")
  String authLoginFailed(String error);

  @Message(id = 285, value = "Login was interrupted")
  String authLoginInterrupted();

  @Message(id = 286, value = "Request to %s failed: %s")
  String authRequestFailed(String url, String reason);

  @Message(id = 287, value = "Request to %s failed with status %d")
  String authRequestFailedWithStatus(String url, int statusCode);

  @Message(id = 288, value = "Response from %s was not valid JSON")
  String authResponseNotJson(String url);

  @Message(id = 289, value = "Unable to save credentials to %s: %s")
  String authCredentialsNotSaved(String path, String reason);

  @Message(id = 290, value = "Unable to read credentials from %s: %s")
  String authCredentialsUnreadable(String path, String reason);

  @Message(id = 291, value = "Unable to delete credentials at %s: %s")
  String authCredentialsNotDeleted(String path, String reason);

  @Message(id = 292, value = "Logged out successfully")
  String authLogoutSuccess();

  @Message(id = 293, value = "Not logged in")
  String authLogoutNotLoggedIn();

  @Message(id = 294, value = "Unable to disable TLS verification: %s")
  String authInsecureTlsFailed(String reason);

  @Message(id = 353, value = "Refusing to send credentials over cleartext HTTP to '%s'.%n"
      + "Use an https:// auth server URL (http:// is allowed only for localhost)")
  String authCleartextHttpBlocked(String url);

  @Message(id = 295, value = "Your session has expired. Run 'streamx auth login' again")
  String authSessionExpired();

  @Message(id = 296, value = "Not logged in. Run 'streamx auth login' first")
  String platformNotLoggedIn();

  @Message(
      id = 297,
      value = "StreamX platform URL is not configured.%nSet it with: streamx settings set %s <url>"
  )
  String platformUrlNotConfigured(String key);

  @Message(id = 298, value = "Not authorized. Run 'streamx auth login' again")
  String platformUnauthorized();

  @Message(id = 301, value = "Request to %s failed: %s")
  String platformRequestFailed(String url, String reason);

  @Message(id = 302, value = "Request to %s failed with status %d")
  String platformRequestFailedWithStatus(String url, int statusCode);

  @Message(id = 303, value = "Request rejected (%d): %s")
  String platformRequestRejected(int statusCode, String detail);

  @Message(id = 354, value = "Refusing to send credentials over cleartext HTTP to '%s'.%n"
      + "Use an https:// platform URL (http:// is allowed only for localhost)")
  String platformCleartextHttpBlocked(String url);

  @Message(id = 309, value = "Stored access token is not a readable JWT")
  String authTokenMalformed();

  @Message(id = 319, value = "Identity provider returned a token response without an access token")
  String authTokenResponseIncomplete();

  @Message(id = 331, value = "Opening your browser to sign in. If it does not open, visit:")
  String authLoginOpeningBrowser();

  @Message(id = 332, value = "No browser available; falling back to device code sign-in.")
  String authBrowserFallbackToDevice();

  @Message(
      id = 333,
      value = "The identity provider does not advertise an authorization endpoint. "
          + "Retry with --no-browser to use the device flow."
  )
  String authCodeFlowUnsupported();

  @Message(id = 334, value = "Could not start the local login listener: %s")
  String authLoopbackFailed(String reason);

  @Message(id = 335, value = "Signed in. You can close this tab and return to the terminal.")
  String authLoopbackSuccess();

  @Message(id = 336, value = "Sign-in failed. Return to the terminal and try again.")
  String authLoopbackDenied();

  @Message(id = 337, value = "Unable to generate a PKCE challenge: %s")
  String authPkceFailed(String reason);

  @Message(id = 338, value = "Configured issuer '%s' does not match discovery document issuer '%s'")
  String authIssuerMismatch(String configured, String documentIssuer);

  @Message(id = 339, value = "Token request rejected (%d): %s")
  String authTokenRequestRejected(int statusCode, String detail);

  @Message(id = 352, value = "The identity provider does not advertise a revocation endpoint")
  String authRevocationUnsupported();


  @Message(id = 306, value = "No organizations found")
  String orgListEmpty();

  @Message(id = 307, value = "Organization '%s' created")
  String orgCreated(String name);

  @Message(id = 308, value = "Organization '%s' deleted")
  String orgDeleted(String orgId);

  @Message(id = 310, value = "No members found")
  String orgMembersListEmpty();

  @Message(id = 311, value = "Member '%s' added with role '%s'")
  String orgMemberAdded(String name, String role);

  @Message(id = 312, value = "Member '%s' removed")
  String orgMemberRemoved(String userId);

  @Message(id = 313, value = "Role of '%s' changed to '%s'")
  String orgMemberRoleChanged(String userId, String role);

  @Message(id = 314, value = "No invitations found")
  String orgInvitationsListEmpty();

  @Message(id = 315, value = "Invitation sent to '%s' with role '%s'")
  String orgInvitationCreated(String email, String role);

  @Message(id = 316, value = "Invitation accepted")
  String orgInvitationAccepted();

  @Message(id = 317, value = "Invitation for '%s' cancelled")
  String orgInvitationCancelled(String email);

  @Message(id = 318, value = "No clusters found")
  String orgClustersListEmpty();

  @Message(id = 320, value = "Paste the invitation token")
  String orgInvitationTokenPrompt();

  @Message(id = 321, value = "Invitation token is required")
  String orgInvitationTokenRequired();

  @Message(id = 322, value = "'%s' is not a member of organization '%s'")
  String orgMemberNotFound(String userId, String orgId);

  @Message(
      id = 323,
      value = "'%s' is a pending invitation (%s), not an active member.%n"
          + "Cancel it with: streamx org invitations cancel %s %s"
  )
  String orgMemberNotActiveForRemoval(String userId, String status, String orgId, String email);

  @Message(
      id = 324,
      value = "'%s' is a pending invitation (%s), not an active member.%n"
          + "Changing its role would grant membership without the invitation being accepted.%n"
          + "Wait for the invitation to be accepted, or add the account directly with:%n"
          + "  streamx org members add %s %s --role <role>"
  )
  String orgMemberNotActiveForRoleChange(String userId, String status, String orgId, String email);

  @Message(id = 325, value = "No projects found")
  String projectListEmpty();

  @Message(id = 326, value = "Project '%s' created (id: %s)")
  String projectCreated(String name, String id);

  @Message(id = 327, value = "Project '%s' updated")
  String projectUpdated(String projectId);

  @Message(id = 328, value = "Project '%s' deleted")
  String projectDeleted(String projectId);

  @Message(id = 329, value = "At least one of --name or --description must be given")
  String projectUpdateNothingToDo();

  @Message(id = 330, value = "No pending changes")
  String projectPendingChangesEmpty();

  @Message(id = 355,
      value = "Invalid profile name '%s'. Use 1-32 lowercase letters, digits or dashes")
  String profileNameInvalid(String name);

  @Message(id = 356,
      value = "Profile '%1$s' does not exist. Create it with: streamx profile create %1$s")
  String profileNotFound(String name);

  @Message(id = 357, value = "Profile '%s' already exists")
  String profileAlreadyExists(String name);

  @Message(id = 358, value = "Profile '%s' created")
  String profileCreated(String name);

  @Message(id = 359, value = "Switched to profile '%s'")
  String profileSwitched(String name);

  @Message(id = 360, value = "Profile '%s' deleted")
  String profileDeleted(String name);

  @Message(id = 361, value = "The profile's stored login was removed locally but NOT revoked.%n"
      + "Next time run 'streamx auth logout' in the profile before deleting it.")
  String profileDeletedLoginNote();

  @Message(id = 362,
      value = "Profile '%s' is set as the current profile. Switch to another profile first")
  String profileCannotDeleteCurrent(String name);

  @Message(id = 363, value = "Profile '%s' is active. Switch to another profile first")
  String profileCannotDeleteActive(String name);

  @Message(id = 364, value = "Current profile: %s")
  String currentProfileHeader(String name);

  @Message(id = 365, value = "Could not create profile '%s': %s")
  String profileCreateFailed(String name, String reason);

  @Message(id = 366, value = "Could not switch profile: %s")
  String profileSwitchFailed(String reason);

  @Message(id = 367, value = "Could not delete profile '%s': %s")
  String profileDeleteFailed(String name, String reason);

  @Message(id = 368, value = "Invalid profile name '%1$s' in %2$s. "
      + "Fix or delete that file, or pass --profile to override")
  String profileInvalidPointer(String name, String pointerFile);

  @Message(id = 369, value = "Profile '%s' does not exist")
  String profileDoesNotExist(String name);

  @Message(id = 370, value = "Auth server URL")
  String profileConfigurePromptAuthUrl();

  @Message(id = 371, value = "Platform API URL")
  String profileConfigurePromptPlatformUrl();

  @Message(id = 372,
      value = "Ingestion URL (per-project on the cloud platform; leave empty to skip)")
  String profileConfigurePromptIngestionUrl();

  @Message(id = 373,
      value = "Verify TLS certificates for %s (answer no for self-signed dev certs)?")
  String profileConfigurePromptVerifyTls(String target);

  @Message(id = 374, value = "A value for '%s' is required")
  String profileConfigureValueRequired(String key);

  @Message(id = 375, value = "Invalid URL '%s'. Use http:// or https://")
  String profileConfigureInvalidUrl(String value);

  @Message(id = 376, value = "Invalid answer '%s'")
  String profileConfigureInvalidAnswer(String value);

  @Message(id = 377, value = "Profile '%s' configured")
  String profileConfigureSaved(String name);

  @Message(id = 378, value = "Log in now?")
  String profileConfigurePromptLogin();

  @Message(id = 379, value = "Login method")
  String profileConfigurePromptLoginMethod();

  @Message(id = 380, value = "Run 'streamx profile configure' to set its endpoints")
  String profileCreateConfigureHint();

  @Message(id = 381, value = "This permanently deletes '%s'. Type the ID to confirm")
  String deleteConfirmPrompt(String id);

  @Message(id = 382, value = "Deletion cancelled: the entered value did not match '%s'")
  String deleteConfirmMismatch(String id);

  @Message(id = 383,
      value = "Deletion needs confirmation. Re-run with --force in non-interactive environments")
  String deleteConfirmRequired();

  @Message(id = 384, value = "No organization given. Pass <orgId>, set STREAMX_ORG, "
      + "or run: streamx profile org use <orgId>")
  String noOrgContext();

  @Message(id = 386, value = "No project given. Pass <projectId>, set STREAMX_PROJECT, "
      + "or run: streamx profile project use <projectId>")
  String noProjectContext();

  @Message(id = 387, value = "Current organization set to '%s'")
  String orgUseSet(String orgId);

  @Message(id = 388, value = "No current organization set. Run: streamx profile org use <orgId>")
  String noCurrentOrg();

  @Message(id = 389, value = "Current project set to '%s'")
  String projectUseSet(String projectId);

  @Message(id = 390, value = "No current project set. Run: streamx profile project use <projectId>")
  String noCurrentProject();

  @Message(id = 391,
      value = "Cleared current project '%s' (it belonged to the previous organization)")
  String orgUseClearedProject(String projectId);

  @Message(id = 392, value = "Current organization (Enter to skip)")
  String profileConfigurePromptOrg();

  @Message(id = 393, value = "Current project (Enter to skip)")
  String profileConfigurePromptProject();

  @Message(id = 394, value = "Skipping organization/project selection: %s")
  String profileConfigureContextSkipped(String reason);

  @Message(id = 395, value = "Current organization cleared")
  String orgUnset();

  @Message(id = 396, value = "Current project cleared")
  String projectUnset();

  @Message(id = 397, value = "Current organization: %s")
  String currentOrgHeader(String orgId);

  @Message(id = 398, value = "Current project: %s")
  String currentProjectHeader(String projectId);

  @Message(id = 399, value = "Project '%s' now runs on: %s")
  String projectClustersSet(String projectId, String clusterIds);

  @Message(id = 400, value = "Cluster '%s' enabled for project '%s'")
  String projectClusterEnabled(String clusterId, String projectId);

  @Message(id = 401, value = "Cluster '%s' disabled for project '%s'")
  String projectClusterDisabled(String clusterId, String projectId);

  @Message(id = 402, value = "Cluster '%s' is already enabled for project '%s'")
  String projectClusterAlreadyEnabled(String clusterId, String projectId);

  @Message(id = 403, value = "Cluster '%s' is already disabled for project '%s'")
  String projectClusterAlreadyDisabled(String clusterId, String projectId);

  @Message(id = 404, value = "Unknown cluster '%s'. Available clusters: %s")
  String projectClusterUnknown(String clusterId, String available);

  @Message(id = 405, value = "Could not read SSH private key file '%s': %s")
  String projectSshKeyFileUnreadable(String path, String reason);

  @Message(id = 406, value = "Repository connected to project '%s'")
  String projectRepoConnected(String projectId);

  @Message(id = 407, value = "Repository settings updated for project '%s'")
  String projectRepoUpdated(String projectId);

  @Message(id = 408, value = "Repository disconnected from project '%s'")
  String projectRepoRemoved(String projectId);

  @Message(id = 409,
      value = "Project '%1$s' has no repository connected. "
          + "Connect one with: streamx project repo set --uri <uri> --branch <branch>")
  String projectRepoNotConnected(String projectId);

  @Message(id = 410, value = "SSH key set for project '%s'")
  String projectSshKeySet(String projectId);

  @Message(id = 411, value = "SSH key removed for project '%s'")
  String projectSshKeyRemoved(String projectId);

  @Message(id = 412, value = "Project '%s' has no SSH key configured")
  String projectSshKeyMissing(String projectId);

  @Message(id = 413, value = "SSH key pair written: '%s' (private) and '%s' (public). "
      + "Add the public key to the Git hosting's deploy keys")
  String projectSshKeyPairWritten(String privatePath, String publicPath);

  @Message(id = 414, value = "Refusing to overwrite existing file '%s'")
  String projectSshKeyFileExists(String path);

  @Message(id = 415, value = "Could not write '%s': %s")
  String projectSshKeyFileWriteFailed(String path, String reason);

  @Message(id = 416, value = "specified")
  String sshKeySpecified();

  @Message(id = 417, value = "not specified")
  String sshKeyNotSpecified();

  @Message(id = 418, value = "not connected")
  String repositoryNotConnected();

  @Message(id = 419, value = "Not found, or you do not have access to it")
  String platformNotFound();

  @Message(id = 420, value = "You do not have permission to perform this action")
  String platformAccessDenied();
}
