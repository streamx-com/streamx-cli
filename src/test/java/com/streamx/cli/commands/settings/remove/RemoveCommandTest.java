package com.streamx.cli.commands.settings.remove;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.streamx.cli.config.StreamxHome;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusMainTest
class RemoveCommandTest {
  private Path configFile;

  @BeforeEach
  void setUp() throws IOException, URISyntaxException {
    Path tempDir = Files.createTempDirectory("RemoveCommandTest");
    System.setProperty("user.home", tempDir.toString());
    configFile = new File(StreamxHome.getConfigUrl().toURI()).toPath();
  }

  @Test
  void shouldRemoveExistingProperty(QuarkusMainLauncher launcher) throws Exception {
    launcher.launch("settings", "set", "a.a.a", "b");
    assertEquals("b", loadProperties().getProperty("a.a.a"));

    LaunchResult launchResult = launcher.launch("settings", "remove", "a.a.a");

    assertNull(loadProperties().getProperty("a.a.a"));
    assertEquals("", launchResult.getOutput());
    assertEquals("", launchResult.getErrorOutput());
    assertEquals(0, launchResult.exitCode());
  }

  @Test
  void shouldSucceedWhenRemovingNonExistentProperty(QuarkusMainLauncher launcher)
      throws Exception {
    LaunchResult launchResult = launcher.launch("settings", "remove", "non.existent.key");

    assertNull(loadProperties().getProperty("non.existent.key"));
    assertEquals("", launchResult.getOutput());
    assertEquals("", launchResult.getErrorOutput());
    assertEquals(0, launchResult.exitCode());
  }

  @Test
  void shouldNotAffectOtherProperties(QuarkusMainLauncher launcher) throws Exception {
    launcher.launch("settings", "set", "a.a.a", "b");
    launcher.launch("settings", "set", "c.c.c", "d");

    LaunchResult launchResult = launcher.launch("settings", "remove", "a.a.a");

    assertNull(loadProperties().getProperty("a.a.a"));
    assertEquals("d", loadProperties().getProperty("c.c.c"));
    assertEquals("", launchResult.getOutput());
    assertEquals("", launchResult.getErrorOutput());
    assertEquals(0, launchResult.exitCode());
  }

  private Properties loadProperties() throws IOException {
    Properties props = new Properties();
    try (InputStream inputStream = Files.newInputStream(configFile)) {
      props.load(inputStream);
    }
    return props;
  }
}