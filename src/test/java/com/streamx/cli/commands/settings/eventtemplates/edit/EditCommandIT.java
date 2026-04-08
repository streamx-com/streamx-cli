package com.streamx.cli.commands.settings.eventtemplates.edit;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.commands.publish.event.UserEventTemplates;
import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
class EditCommandIT extends CliBaseIT {

  @BeforeEach
  void setNoOpEditor() {

    String editor = Files.exists(Path.of("/usr/bin/true")) ? "/usr/bin/true" : "true";
    setEnv(EditCommand.EDITOR, editor);
  }

  @AfterEach
  void clearEditor() {
    clearEnv(EditCommand.EDITOR);
  }

  @Test
  void shouldCopyDefaultIntoUserFolderOnFirstEdit(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "edit",
        "--streamx-home", home.toString(),
        "asset.published"
    );

    result.assertSuccess();

    Path userCopy = home.resolve(UserEventTemplates.DIRECTORY).resolve("asset.published.json");
    assertThat(userCopy).isRegularFile();
    String content = Files.readString(userCopy);
    assertThat(content).contains("com.streamx.blueprints.asset.published.v1");

    Path defaultFile = home.resolve("event-templates/default/asset.published.json");
    assertThat(defaultFile).isRegularFile();
  }

  @Test
  void shouldEditUserTemplateInPlace(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");
    Path userDir = home.resolve(UserEventTemplates.DIRECTORY);
    Files.createDirectories(userDir);
    Path target = userDir.resolve("my.thing.json");
    Files.writeString(target, "{\"type\":\"x\"}");

    ProcessResult result = exec(
        "settings", "event-templates", "edit",
        "--streamx-home", home.toString(),
        "my.thing"
    );

    result.assertSuccess();
    assertThat(target).isRegularFile();
    assertThat(Files.readString(target)).isEqualTo("{\"type\":\"x\"}");
  }

  @Test
  void shouldEditViaPrompt(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = execWithStdin(
        "page.published\n",
        "settings", "event-templates", "edit",
        "--streamx-home", home.toString()
    );

    result.assertSuccess();
    Path userCopy = home.resolve(UserEventTemplates.DIRECTORY).resolve("page.published.json");
    assertThat(userCopy).isRegularFile();
  }

  @Test
  void shouldFailForUnknownName(@TempDir Path tempDir) throws Exception {
    Path home = tempDir.resolve("streamx-home");

    ProcessResult result = exec(
        "settings", "event-templates", "edit",
        "--streamx-home", home.toString(),
        "definitely.not.a.template"
    );

    assertThat(result.exitCode()).isNotZero();
    assertThat(result.stderr()).contains("definitely.not.a.template");
  }
}
