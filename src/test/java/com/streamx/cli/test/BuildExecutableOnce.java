package com.streamx.cli.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

// Build jar or native image before running integration tests
final class BuildExecutableOnce {
  private static volatile boolean done;
  private static volatile boolean success;

  static void ensureBuilt() {
    System.out.println("Building CLI executable before running tests...");

    if (done) {
      assertTrue(success, "Maven build failed in a previous run");
      return;
    }
    synchronized (BuildExecutableOnce.class) {
      if (done) {
        assertTrue(success, "Maven build failed in a previous run");
        return;
      }
      try {
        boolean isNativeImage = Boolean.getBoolean("native.image");
        String mvn = System.getProperty("os.name").toLowerCase().contains("win")
            ? "mvn.cmd"
            : "mvn";
        String[] command = isNativeImage
            ? new String[]{mvn, "clean", "package", "-Pnative", "-DskipTests"}
            : new String[]{mvn, "clean", "package", "-DskipTests"};

        int exitCode = new ProcessBuilder(command)
            .inheritIO()
            .start()
            .waitFor();

        success = exitCode == 0;
      } catch (Exception e) {
        success = false;
      } finally {
        done = true;
      }
      assertTrue(success, "Maven build failed");
    }
  }

  private BuildExecutableOnce() {}
}