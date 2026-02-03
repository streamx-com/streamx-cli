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

  @Message(id = 101, value = "Try '%s%s' for more information on the available options.%n")
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

  @Message(
      id = 110,
      value = """
        Timeout exceeded waiting for the container "%s" after %d seconds.
                
        Try increasing the timeout by setting the """
          + StreamxBaseConfig.PN_CONTAINER_STARTUP_TIMEOUT_SECONDS + " property."
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

  @Message(id = 113, value = """
      StreamX configuration properties:
      =================================
      """)
  String listSettingsHeader();

  @Message(id = 114, value = "No StreamX configuration properties found")
  String listSettingsNoPropertiesFound();
}
