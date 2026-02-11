package com.streamx.cli.framework;

public class CliException extends RuntimeException {
  public CliException(String userFriendlyMessage) {
    super(userFriendlyMessage);
  }

  public CliException(String userFriendlyMessage, Throwable cause) {
    super(userFriendlyMessage, cause);
  }
}
