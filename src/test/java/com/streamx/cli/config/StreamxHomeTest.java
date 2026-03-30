package com.streamx.cli.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

class StreamxHomeTest {

  @TempDir
  Path tempDir;

  @AfterEach
  void cleanup() {
    System.clearProperty("STREAMX_HOME");
    StreamxHome.clearStreamxHomeCliArg();
  }

  @Test
  void shouldUseStreamxHomeEnvVariable() throws Exception {
    try (MockedStatic<StreamxHome> mocked = mockStatic(
        StreamxHome.class, CALLS_REAL_METHODS)) {
      mocked.when(StreamxHome::getStreamxHomeEnv)
          .thenReturn(tempDir.toAbsolutePath().toString());

      URL url = StreamxHome.getConfigUrl();

      Path result = Path.of(url.toURI());
      assertEquals(tempDir.resolve("config/application.properties"), result);
      assertTrue(Files.exists(result));
    }
  }

  @Test
  void shouldUseStreamxHomeSystemProperty() throws Exception {
    System.setProperty("STREAMX_HOME", tempDir.toAbsolutePath().toString());

    URL url = StreamxHome.getConfigUrl();

    Path result = Path.of(url.toURI());
    assertEquals(tempDir.resolve("config/application.properties"), result);
    assertTrue(Files.exists(result), "application.properties should be created");
  }

  @Test
  void shouldFallbackToDefaultWhenNeitherEnvOrSystemPropertyIsSet() throws Exception {
    try (MockedStatic<StreamxHome> mocked = mockStatic(
        StreamxHome.class, CALLS_REAL_METHODS)) {
      mocked.when(StreamxHome::getStreamxHomeEnv).thenReturn(null);

      Path home = StreamxHome.getStreamxHome();

      String homeDir = System.getProperty("user.home");
      assertEquals(Path.of(homeDir, ".streamx"), home);
    }
  }

  @Test
  void shouldCreateConfigDirectoryWhenItDoesNotExist() throws Exception {
    Path homeDir = tempDir.resolve("nested");
    System.setProperty("STREAMX_HOME", homeDir.toAbsolutePath().toString());

    StreamxHome.getConfigUrl();

    Path configDir = homeDir.resolve("config");
    assertTrue(Files.isDirectory(configDir), "Config directory should be created");
    assertTrue(Files.exists(configDir.resolve("application.properties")));
  }
}