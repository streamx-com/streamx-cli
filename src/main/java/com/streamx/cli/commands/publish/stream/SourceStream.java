package com.streamx.cli.commands.publish.stream;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.CliException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class SourceStream {
  public static InputStream get(String source) throws CliException {
    InputStream input;
    if (source != null) {
      try {
        URI uri = URI.create(source);
        if (uri.getScheme() != null) {
          input = uri.toURL().openStream();
        } else {
          input = Files.newInputStream(Path.of(source));
        }
      } catch (Exception e) {
        throw new CliException(msg.unableToOpenSourceInputStream(source), e);
      }
    } else if (System.console() != null) {
      System.err.println(msg.pasteJsonContent());
      input = System.in;
    } else {
      input = System.in;
    }

    try {
      int firstByte = input.read();
      if (firstByte == -1) {
        throw new CliException(msg.inputIsEmpty());
      }

      return new SequenceInputStream(
          new ByteArrayInputStream(new byte[]{(byte) firstByte}),
          input
      );
    } catch (Exception e) {
      throw new CliException(msg.unableToReadInputStream(e.getMessage()), e);
    }
  }
}
