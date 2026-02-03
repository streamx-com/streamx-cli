package com.streamx.cli.commands.settings.set;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.streamx.cli.config.DotStreamxConfigSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class SetCommandTest {
  private String originalUserHome;
  private Path configFile;

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;

  @BeforeEach
  void setUp() throws IOException, URISyntaxException {
    originalUserHome = System.getProperty("user.home");
    Path tempDir = Files.createTempDirectory("SetCommandTest");
    System.setProperty("user.home", tempDir.toString());
    configFile = new File(DotStreamxConfigSource.getUrl().toURI()).toPath();

    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
  }

  @AfterEach
  void tearDown() {
    System.setProperty("user.home", originalUserHome);

    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  @Test
  void shouldSetNewProperty() throws Exception {
    SetCommand command = new SetCommand();
    CommandLine cmd = new CommandLine(command);

    cmd.parseArgs("a", "b");
    command.runCommand();

    cmd.parseArgs("x", "y");
    command.runCommand();

    assertEquals("b", loadProperties().getProperty("a"));
    assertEquals("y", loadProperties().getProperty("x"));

    assertEquals("", outContent.toString());
    assertEquals("", errContent.toString());
  }

  @Test
  void shouldUpdateExistingProperty() throws Exception {
    SetCommand command = new SetCommand();
    CommandLine cmd = new CommandLine(command);

    cmd.parseArgs("a", "b");
    command.runCommand();

    assertEquals("b", loadProperties().getProperty("a"));

    cmd.parseArgs("a", "c");
    command.runCommand();
    assertEquals("c", loadProperties().getProperty("a"));

    assertEquals("", outContent.toString());
    assertEquals("", errContent.toString());
  }

  private Properties loadProperties() throws IOException {
    Properties props = new Properties();
    try (var in = Files.newInputStream(configFile)) {
      props.load(in);
    }
    return props;
  }
}