package com.streamx.cli.commands.publish.stream;

import com.streamx.cli.framework.CliException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;

import static com.streamx.cli.i18n.MessageProvider.msg;

public class SourceStream {
  public static InputStream get(String source) throws CliException {
    InputStream input;
    if (source != null) {
      try {
        URI uri = URI.create(source);
        if (uri.getScheme() != null) {
          input = uri.toURL().openStream();
        } else {
          input = java.nio.file.Files.newInputStream(java.nio.file.Path.of(source));
        }
      } catch (CliException e) {
        throw e;
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
    } catch (CliException e) {
      throw e;
    } catch (Exception e) {
      throw new CliException(msg.unableToReadInputStream(e.getMessage()), e);
    }
  }
}
