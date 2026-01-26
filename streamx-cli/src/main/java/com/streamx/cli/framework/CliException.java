package com.streamx.cli.framework;

public class CliException extends Exception {
  public CliException(String userFriendlyMessage) {
    super(userFriendlyMessage);
  }

  public CliException(String userFriendlyMessage, Throwable cause) {
    super(userFriendlyMessage, cause);
  }
}
