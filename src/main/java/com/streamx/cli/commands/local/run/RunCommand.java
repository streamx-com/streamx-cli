package com.streamx.cli.commands.local.run;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.meshprocessing.MeshManager;
import com.streamx.cli.util.BannerPrinter;
import com.streamx.runner.StreamxRunner;
import com.streamx.runner.exception.ContainerStartupTimeoutException;
import jakarta.inject.Inject;
import java.nio.file.Path;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "run",
    mixinStandardHelpOptions = true,
    description = "Run a StreamX Mesh locally.")
public class RunCommand extends AbstractSilentCommand {
  @CommandLine.Option(
      names = {"-f", "--file"},
      description = "Path to mesh definition file.",
      defaultValue = "mesh.yaml"
  )
  public Path meshPath;

  @Inject
  StreamxRunner runner;

  @Inject
  MeshManager meshManager;

  @Override
  public CommandResult<Void> runCommand() {
    if (!meshPath.toFile().exists()) {
      throw new CliException(msg.meshFileNotFound(meshPath.toString()));
    }

    try {
      meshManager.initializeMesh(meshPath);

      BannerPrinter.printBanner();
      meshManager.initializeRunMode(meshPath);

      meshManager.start();

      return new CommandResult<>(null);
    } catch (ContainerStartupTimeoutException e) {
      String errMessage = msg.dockerContainerStartupFailed(
          e.getContainerName(),
          runner.getContext().getStreamxBaseConfig().getContainerStartupTimeout()
      );
      throw new CliException(errMessage, e);
    }
  }
}
