package com.streamx.cli.i18n;

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
}
