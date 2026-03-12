package com.streamx.cli.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

// Resolve jar or native image path before running integration tests
final class BuildExecutableOnce {
  private static final boolean NATIVE = Boolean.getBoolean("native.image");
  private static final Path TARGET = Path.of("target");
  private static volatile boolean done;
  private static volatile boolean success;
  private static List<String> resolvedCommand;

  static void ensureBuilt() {
    if (done) {
      assertTrue(success, "Executable resolution failed in a previous run");
      return;
    }
    synchronized (BuildExecutableOnce.class) {
      if (done) {
        assertTrue(success, "Executable resolution failed in a previous run");
        return;
      }
      try {
        resolvedCommand = resolveExecutablePath();
        success = true;
      } finally {
        done = true;
      }
    }
  }

  static List<String> getExecutablePath() {
    assertTrue(done && success, "ensureBuilt() must be called before getExecutablePath()");
    return resolvedCommand;
  }

  private static List<String> resolveExecutablePath() {
    if (NATIVE) {
      Path executable = findNativeExecutable();
      assertTrue(Files.isExecutable(executable),
          "Native executable not found in %s. Run 'mvn package -Pnative -DskipTests' first"
              .formatted(TARGET));
      return List.of(executable.toAbsolutePath().toString());
    } else {
      Path jar = TARGET.resolve("quarkus-app/quarkus-run.jar");
      assertTrue(jar.toFile().exists(),
          "JAR not found at %s. Run 'mvn package -DskipTests' first".formatted(jar));
      return List.of("java", "-jar", jar.toAbsolutePath().toString());
    }
  }

  private static Path findNativeExecutable() {
    try (Stream<Path> files = Files.list(TARGET)) {
      return files
          .filter(p -> p.getFileName().toString().endsWith("-runner"))
          .filter(Files::isExecutable)
          .findFirst()
          .orElse(TARGET.resolve("*-runner"));
    } catch (Exception e) {
      return TARGET.resolve("*-runner");
    }
  }

  private BuildExecutableOnce() {}
}