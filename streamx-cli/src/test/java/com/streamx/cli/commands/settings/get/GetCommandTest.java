package com.streamx.cli.commands.settings.get;

import static com.streamx.cli.i18n.MessageProvider.msg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamx.cli.config.DotStreamxConfigSource;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
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

class GetCommandTest {
  private String originalUserHome;
  private Path configFile;

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;

  String[][] existingProperties = {
      {"test.key", "test.value"},
      {"another.key", "another.value"},
      {"special.chars", "value=with:special@chars!"},
      {"empty.value", ""},
      {"spaced.value", "value with spaces"}
  };

  @BeforeEach
  void setUp() throws IOException, URISyntaxException {
    originalUserHome = System.getProperty("user.home");
    Path tempDir = Files.createTempDirectory("SetCommandTest");
    System.setProperty("user.home", tempDir.toString());
    configFile = new File(DotStreamxConfigSource.getUrl().toURI()).toPath();

    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));

    Properties initialProps = new Properties();
    for (String[] property : existingProperties) {
      initialProps.setProperty(property[0], property[1]);
    }

    try (var out = Files.newOutputStream(configFile)) {
      initialProps.store(out, null);
    }
  }

  @AfterEach
  void tearDown() {
    System.setProperty("user.home", originalUserHome);

    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  @Test
  void shouldGetExistingProperties() throws Exception {
    for (String[] property : existingProperties) {
      String key = property[0];
      String value = property[1];

      outContent.reset();
      errContent.reset();

      GetCommand command = new GetCommand();
      CommandLine cmd = new CommandLine(command);
      cmd.parseArgs(key);

      CommandResult<GetCommandResult> result = command.runCommand();

      assertEquals(key, result.result.key());
      assertEquals(value, result.result.value());

      String textOutput = command.getTextOutput(result);
      assertEquals(value, textOutput);

      assertEquals("", errContent.toString());
    }
  }

  @Test
  void shouldThrowExceptionWhenPropertyNotFound() {
    GetCommand command = new GetCommand();
    CommandLine cmd = new CommandLine(command);
    String key = "nonexistent";
    cmd.parseArgs(key);

    CliException exception = assertThrows(CliException.class, command::runCommand);

    assertEquals(exception.getMessage(), msg.noSettingsPropertyFound(key));
  }
}