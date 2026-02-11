package com.streamx.cli.commands.settings.set;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.streamx.cli.config.DotStreamxConfigSource;
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
class SetCommandTest {
  private Path configFile;

  @BeforeEach
  void setUp() throws IOException, URISyntaxException {
    Path tempDir = Files.createTempDirectory("SetCommandTest");
    System.setProperty("user.home", tempDir.toString());
    configFile = new File(DotStreamxConfigSource.getUrl().toURI()).toPath();
  }

  @Test
  void shouldSetNewProperty(QuarkusMainLauncher launcher) throws Exception {
    LaunchResult launchResult = launcher.launch("settings", "set", "a.a.a", "b");

    assertEquals("b", loadProperties().getProperty("a.a.a"));
    assertEquals("", launchResult.getOutput());
    assertEquals("", launchResult.getErrorOutput());
    assertEquals(0, launchResult.exitCode());
  }

  @Test
  void shouldUpdateExistingProperty(QuarkusMainLauncher launcher) throws Exception {
    LaunchResult launchResult1 = launcher.launch("settings", "set", "a.a.a", "b");

    assertEquals("b", loadProperties().getProperty("a.a.a"));
    assertEquals("", launchResult1.getOutput());
    assertEquals("", launchResult1.getErrorOutput());
    assertEquals(0, launchResult1.exitCode());

    LaunchResult launchResult2 = launcher.launch("settings", "set", "a.a.a", "c");

    assertEquals("c", loadProperties().getProperty("a.a.a"));
    assertEquals("", launchResult2.getOutput());
    assertEquals("", launchResult2.getErrorOutput());
    assertEquals(0, launchResult2.exitCode());
  }

  private Properties loadProperties() throws IOException {
    Properties props = new Properties();
    try (InputStream inputStream = Files.newInputStream(configFile)) {
      props.load(inputStream);
    }
    return props;
  }
}