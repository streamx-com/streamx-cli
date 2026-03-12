package com.streamx.cli.test;

import com.streamx.cli.mesh.MeshManager;
import com.streamx.cli.test.profiles.DefaultMeshTestProfile;
import com.streamx.cli.test.profiles.MeshWithAuthTestProfile;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
@IfBuildProfile(anyOf = {
    DefaultMeshTestProfile.PROFILE_NAME,
    MeshWithAuthTestProfile.PROFILE_NAME
})
public class MeshTestEnv {

  private static final Logger LOG = Logger.getLogger(MeshTestEnv.class);

  public static final String MESH_PATH_CONFIG = "test.mesh.path";

  private static volatile String capturedToken;
  private static final CountDownLatch tokenLatch = new CountDownLatch(1);

  @Inject
  MeshManager meshManager;

  @ConfigProperty(name = MESH_PATH_CONFIG)
  String meshPath;

  void onStart(@Observes StartupEvent ev) {
    captureAuthToken();

    Path path = Paths.get(meshPath);
    meshManager.initializeMesh(path);
    meshManager.initializeRunMode(path);
    meshManager.start();
  }

  /**
   * Blocks until the token is captured or timeout is reached.
   */
  public String awaitAuthToken() {
    try {
      if (!tokenLatch.await(2, TimeUnit.MINUTES)) {
        throw new IllegalStateException("Timed out waiting for JWT token from startup logs");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for JWT token", e);
    }
    return capturedToken;
  }

  private void captureAuthToken() {
    PrintStream originalOut = System.out;
    PrintStream interceptor = new java.io.PrintStream(originalOut) {
      @Override
      public void println(String x) {
        if (x != null && capturedToken == null) {
          Matcher matcher = java.util.regex.Pattern
              .compile("cli token: ([A-Za-z0-9._\\-]+)")
              .matcher(x);
          if (matcher.find()) {
            capturedToken = matcher.group(1);
            tokenLatch.countDown();
            System.setOut(originalOut); // restore
          }
        }
        super.println(x);
      }
    };
    System.setOut(interceptor);
  }

  void onStop(@Observes ShutdownEvent ev) {
    LOG.info("Stopping mesh after tests...");
    try {
      meshManager.stop();
    } catch (Exception e) {
      LOG.error("Error during stopping mesh", e);
    }
  }
}