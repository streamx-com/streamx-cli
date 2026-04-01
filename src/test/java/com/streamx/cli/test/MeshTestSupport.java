package com.streamx.cli.test;

import com.streamx.cli.mesh.MeshManager;
import io.quarkus.arc.Arc;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages mesh lifecycle for integration tests.
 * Each test class that needs a mesh calls {@link #startMesh(String)} in @BeforeAll
 * and {@link #stopMesh()} in @AfterAll.
 * Each invocation gets a unique container name prefix and random free ports.
 * Containers are cleaned up by Ryuk after the JVM exits.
 */
public final class MeshTestSupport {

  private static volatile MeshManager activeMeshManager;
  private static volatile int activeProxyPort;
  private static volatile int activePulsarHttpPort;
  private static volatile String capturedToken;
  private static CountDownLatch tokenLatch;

  private MeshTestSupport() {
  }

  private static int freePort() {
    try (ServerSocket s = new ServerSocket(0)) {
      return s.getLocalPort();
    } catch (IOException e) {
      throw new RuntimeException("Failed to find a free port", e);
    }
  }

  public static void startMesh(String meshYamlPath) {
    String jvmId = UUID.randomUUID().toString().substring(0, 4);
    String prefix = "sx-" + jvmId + "-";

    activeProxyPort = freePort();
    activePulsarHttpPort = freePort();
    int pulsarBrokerPort = freePort();
    int gatewayHttpPort = freePort();
    int gatewayAdminPort = freePort();

    System.setProperty("streamx.runner.mesh-name-prefix", prefix);
    System.setProperty("streamx.runner.pulsar.broker-port",
        String.valueOf(pulsarBrokerPort));
    System.setProperty("streamx.runner.pulsar.http-port",
        String.valueOf(activePulsarHttpPort));
    System.setProperty("streamx.runner.gateway.http-port",
        String.valueOf(gatewayHttpPort));
    System.setProperty("streamx.runner.gateway.admin-port",
        String.valueOf(gatewayAdminPort));
    System.setProperty("streamx.ingestion.url",
        "http://localhost:" + activeProxyPort);
    System.setProperty("test.proxy.host-port",
        String.valueOf(activeProxyPort));

    capturedToken = null;
    tokenLatch = new CountDownLatch(1);
    captureAuthToken();

    MeshManager meshManager =
        Arc.container().select(MeshManager.class).get();
    Path path = Paths.get(meshYamlPath);
    meshManager.initializeMesh(path);
    meshManager.initializeRunMode(path);
    meshManager.start();

    activeMeshManager = meshManager;
  }

  public static void stopMesh() {
    if (activeMeshManager != null) {
      try {
        activeMeshManager.stop();
      } catch (Exception e) {
        System.err.println("Error stopping mesh: " + e.getMessage());
      }
      activeMeshManager = null;
    }
  }

  public static boolean isMeshActive() {
    return activeMeshManager != null;
  }

  public static int getProxyPort() {
    return activeProxyPort;
  }

  public static int getPulsarHttpPort() {
    return activePulsarHttpPort;
  }

  public static String awaitAuthToken() {
    try {
      if (!tokenLatch.await(2, TimeUnit.MINUTES)) {
        throw new IllegalStateException(
            "Timed out waiting for JWT token from startup logs");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Interrupted while waiting for JWT token", e);
    }
    return capturedToken;
  }

  private static void captureAuthToken() {
    PrintStream originalOut = System.out;
    PrintStream interceptor = new PrintStream(originalOut) {
      @Override
      public void println(String x) {
        if (x != null && capturedToken == null) {
          Matcher matcher = Pattern
              .compile("cli token: ([A-Za-z0-9._\\-]+)")
              .matcher(x);
          if (matcher.find()) {
            capturedToken = matcher.group(1);
            tokenLatch.countDown();
            System.setOut(originalOut);
          }
        }
        super.println(x);
      }
    };
    System.setOut(interceptor);
  }
}
