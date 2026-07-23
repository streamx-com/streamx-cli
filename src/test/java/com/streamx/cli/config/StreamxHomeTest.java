package com.streamx.cli.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    StreamxHome.clearProfileCliArg();
  }

  @Test
  void shouldUseStreamxHomeEnvVariable() throws Exception {
    try (MockedStatic<StreamxHome> mocked = mockStatic(
        StreamxHome.class, CALLS_REAL_METHODS)) {
      mocked.when(StreamxHome::getStreamxHomeEnv)
          .thenReturn(tempDir.toAbsolutePath().toString());

      StreamxHome.createConfigIfNotExists();
      URL url = StreamxHome.getConfigUrl();

      Path result = Path.of(url.toURI());
      assertEquals(tempDir.resolve("profiles/default/config/application.properties"), result);
      assertTrue(Files.exists(result));
    }
  }

  @Test
  void shouldUseStreamxHomeSystemProperty() throws Exception {
    System.setProperty("STREAMX_HOME", tempDir.toAbsolutePath().toString());

    StreamxHome.createConfigIfNotExists();
    URL url = StreamxHome.getConfigUrl();

    Path result = Path.of(url.toURI());
    assertEquals(tempDir.resolve("profiles/default/config/application.properties"), result);
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

    StreamxHome.createConfigIfNotExists();

    Path configDir = homeDir.resolve("profiles/default/config");
    assertTrue(Files.isDirectory(configDir), "Config directory should be created");
    assertTrue(Files.exists(configDir.resolve("application.properties")));
  }

  @Test
  void shouldApplySettingsToSystemProperties() throws Exception {
    System.setProperty("STREAMX_HOME", tempDir.toAbsolutePath().toString());
    String key = "streamx.runner.observability.enabled";
    String otherKey = "streamx.runner.gateway.http-port";
    System.clearProperty(key);
    System.clearProperty(otherKey);
    try {
      Path configDir = tempDir.resolve("profiles/default/config");
      Files.createDirectories(configDir);
      Files.writeString(configDir.resolve("application.properties"),
          key + "=true\n" + otherKey + "=8081\n");

      StreamxHome.applySettingsToSystemProperties();

      assertEquals("true", System.getProperty(key));
      assertEquals("8081", System.getProperty(otherKey));
    } finally {
      System.clearProperty(key);
      System.clearProperty(otherKey);
    }
  }

  @Test
  void shouldNotOverrideExplicitlySetSystemProperty() throws Exception {
    System.setProperty("STREAMX_HOME", tempDir.toAbsolutePath().toString());
    String key = "streamx.runner.gateway.http-port";
    System.setProperty(key, "9999");
    try {
      Path configDir = tempDir.resolve("profiles/default/config");
      Files.createDirectories(configDir);
      Files.writeString(configDir.resolve("application.properties"), key + "=8081\n");

      StreamxHome.applySettingsToSystemProperties();

      assertEquals("9999", System.getProperty(key));
    } finally {
      System.clearProperty(key);
    }
  }

  @Test
  void shouldClearStaleAppliedKeysOnReapply() throws Exception {
    System.setProperty("STREAMX_HOME", tempDir.toAbsolutePath().toString());
    String key = "streamx.runner.gateway.http-port";
    System.clearProperty(key);
    try {
      Path configDir = tempDir.resolve("profiles/default/config");
      Files.createDirectories(configDir);
      Path configFile = configDir.resolve("application.properties");

      Files.writeString(configFile, key + "=8081\n");
      StreamxHome.applySettingsToSystemProperties();
      assertEquals("8081", System.getProperty(key));

      Files.writeString(configFile, "");
      StreamxHome.applySettingsToSystemProperties();
      assertNull(System.getProperty(key),
          "key applied by previous call should be cleared on re-apply");
    } finally {
      System.clearProperty(key);
    }
  }

  @Test
  void shouldNoOpWhenConfigFileMissing() throws Exception {
    System.setProperty("STREAMX_HOME", tempDir.toAbsolutePath().toString());
    String key = "streamx.test.key.that.should.not.exist";
    System.clearProperty(key);

    StreamxHome.applySettingsToSystemProperties();

    assertNull(System.getProperty(key));
  }
}
