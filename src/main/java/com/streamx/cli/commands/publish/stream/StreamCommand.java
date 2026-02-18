package com.streamx.cli.commands.publish.stream;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.meshprocessing.MeshManager;
import com.streamx.runner.StreamxRunner;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "stream",
    mixinStandardHelpOptions = true,
    description = "Stream a bunch of events")
public class StreamCommand extends AbstractSilentCommand {
  @CommandLine.Parameters(
      index = "0",
      description = "Path to mesh definition file.",
      arity = "0..1",
      defaultValue = CommandLine.Parameters.NULL_VALUE
  )
  public URI source;

  @Inject
  StreamxRunner runner;

  @Inject
  MeshManager meshManager;

  @Override
  public CommandResult<Void> runCommand() {
    try {
      InputStream input = getInput();

      return new CommandResult<>(null);
    } catch (CliException e) {
      throw e;
    } catch (Exception e) {
      throw new CliException("Unable to stream" + e.getMessage(), e);
    }
  }

  private InputStream getInput() throws CliException {
    InputStream input;
    if (source == null) {
      input = System.in;
    } else {
      try {
        input = source.toURL().openStream();
      } catch (Exception e) {
        throw new CliException("Unable to open source input stream: " + source, e);
      }

    }

    try {
      int firstByte = input.read();
      if (firstByte == -1) {
        throw new CliException("Input is empty.");
      }

      return new SequenceInputStream(
          new ByteArrayInputStream(new byte[]{(byte) firstByte}),
          input
      );
    } catch (Exception e) {
      throw new CliException("Unable to read input stream: " + e.getMessage(), e);
    }
  }
}
