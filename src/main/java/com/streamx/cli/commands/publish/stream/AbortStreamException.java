package com.streamx.cli.commands.publish.stream;

import com.streamx.cli.framework.CliException;

class AbortStreamException extends RuntimeException {
  AbortStreamException(CliException cause) {
    super(cause);
  }
}
