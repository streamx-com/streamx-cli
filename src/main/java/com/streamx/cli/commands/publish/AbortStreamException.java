package com.streamx.cli.commands.publish;

import com.streamx.cli.framework.CliException;

public class AbortStreamException extends RuntimeException {
  public AbortStreamException(CliException cause) {
    super(cause);
  }
}
