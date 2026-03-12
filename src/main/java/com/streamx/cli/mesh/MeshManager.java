package com.streamx.cli.mesh;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static com.streamx.cli.util.Output.print;
import static com.streamx.runner.main.Main.StreamxApp.printSummary;

import com.streamx.cli.framework.CliException;
import com.streamx.cli.util.ExceptionUtils;
import com.streamx.cli.util.ExecutionExceptionHandler;
import com.streamx.mesh.model.ServiceMesh;
import com.streamx.runner.StreamxRunner;
import com.streamx.runner.event.MeshReloadUpdate;
import com.streamx.runner.validation.excpetion.DockerContainerNonUniqueException;
import com.streamx.runner.validation.excpetion.DockerEnvironmentException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

@ApplicationScoped
public class MeshManager {

  @Inject
  StreamxRunner runner;

  @Inject
  MeshDefinitionResolver meshDefinitionResolver;

  @Inject
  ExecutionExceptionHandler executionExceptionHandler;

  private ErrorHandlingExecutor errorHandlingExecutor;
  private Path meshPath;
  private String meshPathAsString;
  private Path normalizedMeshPath;
  private CommandLine commandLine;
  private ServiceMesh serviceMesh;
  private boolean firstStart = true;

  public void initializeMesh(Path meshPath) {
    this.meshPath = meshPath;
    this.normalizedMeshPath = meshPath.toAbsolutePath().normalize();
    this.meshPathAsString = normalizedMeshPath.toString();

    this.serviceMesh = resolveMeshDefinition(meshPath);;
  }

  public void initializeRunMode(Path meshPath) {
    print(msg.settingUpSystemContainers());
    this.errorHandlingExecutor =
        new ErrorHandlingExecutor(false, executionExceptionHandler, commandLine);

    try {
      this.runner.initialize(serviceMesh, meshPathAsString);
    } catch (DockerContainerNonUniqueException e) {
      throw new CliException(msg.failedToStartMeshContainers(e.getMessage()));
    } catch (DockerEnvironmentException e) {
      throw new CliException(msg.invalidDockerEnvironment());
    } catch (Exception e) {
      throw getMeshException(meshPath, e);
    }

    this.runner.startBase();
  }

  public void start() {
    if (firstStart) {
      firstStart = false;
    }
    errorHandlingExecutor.execute(this::doStart);
  }

  private void doStart() {
    this.serviceMesh = resolveMeshDefinition(meshPath);

    try {
      this.runner.initialize(serviceMesh, meshPathAsString);
    } catch (DockerContainerNonUniqueException e) {
      throw new CliException(msg.failedToStartMeshContainers(e.getMessage()));
    } catch (DockerEnvironmentException e) {
      throw new CliException(msg.invalidDockerEnvironment());
    } catch (Exception e) {
      throw getMeshException(meshPath, e);
    }

    print("");
    print(msg.startingMesh());

    boolean failFast = !errorHandlingExecutor.failsafe;
    boolean started = this.runner.startMesh(failFast);
    print("");
    if (started) {
      printSummary(runner, normalizedMeshPath, serviceMesh);
    }
  }

  @NotNull
  private ServiceMesh resolveMeshDefinition(Path meshPath) {
    try {
      return meshDefinitionResolver.resolve(meshPath);
    } catch (Exception e) {
      throw getMeshException(meshPath, e);
    }
  }

  private CliException getMeshException(Path meshPath, Exception e) {
    String message = msg.unableToReadMeshDefinition(meshPath.toString(), e.getMessage());
    return new CliException(ExceptionUtils.appendLogSuggestion(message), e);
  }

  public void stop() {
    this.serviceMesh = null;
    doStop();
  }

  private void doStop() {
    try {
      print(msg.stoppingMesh());

      runner.stopMesh();
      runner.stopBase();

      print(msg.meshStopped());
      print("");
    } catch (Exception e) {
      if (!errorHandlingExecutor.failsafe) {
        throw ExceptionUtils.sneakyThrow(e);
      }
    }
  }

  public void reload() {
    ServiceMesh newServiceMesh = errorHandlingExecutor.execute(() -> {
      ServiceMesh serviceMesh = resolveMeshDefinition(meshPath);
      serviceMesh.validate().assertValid();

      return serviceMesh;
    });

    if (newServiceMesh == null) {
      print("\n" + msg.meshDefinitionIsInvalidSkipReload());
      return;
    }

    if (firstStart) {
      firstStart = false;
      start();
    } else {
      try {
        runner.reloadMesh(newServiceMesh);
        serviceMesh = newServiceMesh;
      } catch (Exception e) {
        serviceMesh = null;
        print(msg.meshReloadFailed());
        throw e;
      }
    }
  }

  void onMeshStarted(@Observes MeshReloadUpdate event) {
    switch (event.event()) {
      case MESH_UNCHANGED -> print("\n" + msg.meshDefinitionIsUnchangedSkipReload());
      case FULL_RELOAD_STARTED -> print("\n" + msg.meshFileChangedFullReload());
      case INCREMENTAL_RELOAD_STARTED -> print("\n" + msg.meshFileChangedIncrementalReload());
      case FULL_RELOAD_FINISHED, INCREMENTAL_RELOAD_FINISHED -> print("\n" + msg.meshReloaded());
      case FULL_RELOAD_FAILED, INCREMENTAL_RELOAD_FAILED -> print("\n" + msg.meshReloadFailed());
      default -> { }
    }
  }

  private static class ErrorHandlingExecutor {

    private final boolean failsafe;
    private final ExecutionExceptionHandler executionExceptionHandler;
    private final CommandLine commandLine;

    public ErrorHandlingExecutor(boolean failsafe,
        ExecutionExceptionHandler executionExceptionHandler, CommandLine commandLine) {
      this.failsafe = failsafe;
      this.executionExceptionHandler = executionExceptionHandler;
      this.commandLine = commandLine;
    }

    private <T> T execute(Callable<T> callable) {
      try {
        return callable.call();
      } catch (Exception e) {
        if (failsafe) {
          executionExceptionHandler.handleExecutionException(e, commandLine);

          return null;
        } else {
          throw ExceptionUtils.sneakyThrow(e);
        }
      }
    }

    private void execute(Runnable runnable) {
      try {
        runnable.run();
      } catch (Exception e) {
        if (failsafe) {
          executionExceptionHandler.handleExecutionException(e, commandLine);
        } else {
          throw ExceptionUtils.sneakyThrow(e);
        }
      }
    }
  }
}
