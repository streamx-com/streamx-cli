package com.streamx.cli.commands.publish.events;

import static com.streamx.cli.commands.publish.events.TestDirectoryGenerator.DEFAULT_TEMPLATE;
import static com.streamx.cli.commands.publish.events.TestDirectoryGenerator.INVALID_PATCH;
import static com.streamx.cli.commands.publish.events.TestDirectoryGenerator.INVALID_TEMPLATE;
import static com.streamx.cli.commands.publish.events.TestDirectoryGenerator.PAYLOAD_PATH_TEMPLATE;
import static com.streamx.cli.commands.publish.events.TestDirectoryGenerator.TYPE_PATCH;
import static com.streamx.cli.i18n.MessageProvider.msg;
import static com.streamx.cli.test.MeshAssertions.assertEventsPublished;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.annotation.DisabledIfDockerUnavailable;
import com.streamx.cli.test.profiles.DefaultMeshTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
@DisabledIfDockerUnavailable
@TestProfile(DefaultMeshTestProfile.class)
public class EventsCommandIT extends CliBaseIT {

  @Test
  void shouldPrintHelpInformation() throws Exception {
    ProcessResult result = exec("publish", "events", "--help");

    assertThat(result.stdout())
        .contains("Publishes multiple events based on a directory structure");
    assertThat(result.stderr()).isEmpty();
  }

  @Nested
  class Validation {

    @Test
    void shouldFailWhenRootTemplateIsMissing(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withFiles(2)
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString());

      result.assertExitCode(1);
      assertThat(result.stderr()).contains(msg.noEventTemplateInsideDirectory(tempDir.toString()));
    }

    @Test
    void shouldFailWhenRootTemplateIsCorrupted(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(INVALID_TEMPLATE)
          .withFiles(1)
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString());

      result.assertExitCode(1);
      assertThat(result.stderr()).contains(msg.eventTemplateCorrupted(tempDir.toString()));
      assertEventsPublished(0);
    }
  }

  @Nested
  class BasicPublishing {

    @Test
    void shouldPublishSingleFile(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withFiles("page.json")
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString());

      result.assertSuccess();
      assertEventsPublished(1);
      assertThat(result.stdout()).contains(msg.eventsPublished(1));
    }

    @Test
    void shouldPublishExactlyNFiles(@TempDir Path tempDir) throws Exception {
      int n = 7;
      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withFiles(n)
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString());

      result.assertSuccess();
      assertEventsPublished(n);
      assertThat(result.stdout()).contains(msg.eventsPublished(n));
    }

    @Test
    void shouldNotPublishEventTemplateFileAsPayload(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withFiles("a.json", "b.json")
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString());

      result.assertSuccess();
      assertEventsPublished(2);
      assertThat(result.stdout()).contains(msg.eventsPublished(2));
    }

    @Test
    void shouldNotPublishPatchFileAsPayload(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withPatchFile("sale", TYPE_PATCH)
          .withFiles(2)
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString());

      result.assertSuccess();
      assertEventsPublished(2);
    }
  }

  @Nested
  class RecursiveProcessing {

    @Test
    void shouldPublishFilesFromSubDirectories(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withFiles(2)
          .withSubDirectory("sub", sub -> sub
              .withFiles(3))
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString());

      result.assertSuccess();
      assertEventsPublished(5);
      assertThat(result.stdout()).contains(msg.eventsPublished(5));
    }

    @Test
    void shouldOverrideTemplateForSubDirectory(@TempDir Path tempDir) throws Exception {
      String subTemplate = DEFAULT_TEMPLATE.replace(
          "com.streamx.blueprints.page.published.v1",
          "com.streamx.blueprints.data.published.v1"
      );

      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withFiles(1)
          .withSubDirectory("cats", sub -> sub
              .withEventTemplate(subTemplate)
              .withFiles(2))
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString());

      result.assertSuccess();
      assertEventsPublished(3);
    }

    @Test
    void shouldProcessDeeplyNestedDirectories(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withSubDirectory("level1", l1 -> l1
              .withFiles(1)
              .withSubDirectory("level2", l2 -> l2
                  .withFiles(1)
                  .withSubDirectory("level3", l3 -> l3
                      .withFiles(1))))
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString());

      result.assertSuccess();
      assertEventsPublished(3);
    }
  }

  @Nested
  class PlaceholderResolution {

    @Test
    void shouldResolveRelativePath(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withSubDirectory("products", sub -> sub
              .withFiles("chair.json"))
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString());

      result.assertSuccess();
      assertEventsPublished(1);
    }

    @Test
    void shouldResolvePayloadPath(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(PAYLOAD_PATH_TEMPLATE)
          .withFiles("item.json")
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString());

      result.assertSuccess();
      assertEventsPublished(1);
    }
  }

  @Nested
  class PatchSupport {

    @Test
    void shouldApplyPatchWhenFileIsPresent(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withPatchFile("sale", TYPE_PATCH)
          .withFiles(2)
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString(), "--patch", "sale");

      result.assertSuccess();
      assertEventsPublished(2);
    }

    @Test
    void shouldContinueWithoutPatchWhenUserConfirms(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withFiles(1)
          .build(tempDir);

      ProcessResult result = execWithStdin(
          "y\n",
          "publish", "events", tempDir.toString(), "--patch", "missing-patch");

      result.assertSuccess();
      assertEventsPublished(1);
      assertThat(result.stderr()).contains(
          msg.patchNotFound("missing-patch", tempDir.toString()));
    }

    @Test
    void shouldAbortWhenPatchMissingAndUserDeclines(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withFiles(3)
          .build(tempDir);

      ProcessResult result = execWithStdin(
          "n\n",
          "publish", "events", tempDir.toString(), "--patch", "missing-patch");

      result.assertExitCode(0);
      assertEventsPublished(0);
    }

    @Test
    void shouldFailWhenPatchJsonIsInvalid(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withPatchFile("bad", INVALID_PATCH)
          .withFiles(1)
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString(), "--patch", "bad");

      result.assertExitCode(1);
      assertThat(result.stderr()).contains(msg.patchIsInvalid("bad"));
      assertEventsPublished(0);
    }
  }

  @Nested
  class BatchPublishing {

    @Test
    void shouldPublishEventsInBatches(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withFiles(11)
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(), "--batch-size", "3");

      result.assertSuccess();
      assertEventsPublished(11);
      assertThat(result.stdout()).contains(
          msg.streamBatchPublishingCompleted(11, 11, 0, 0, 4, 4, 0));
    }

    @Test
    void shouldFlushRemainderBatchAtEnd(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withFiles(5)
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(), "--batch-size", "3");

      result.assertSuccess();
      assertEventsPublished(5);
    }

    @Test
    void shouldPublish100FilesInBatchesOf10(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withFiles(100)
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(), "--batch-size", "10");

      result.assertSuccess();
      assertEventsPublished(100);
      assertThat(result.stdout()).contains(
          msg.streamBatchPublishingCompleted(100, 100, 0, 0, 10, 10, 0));
    }

    @Test
    void shouldFailOnFirstFailedBatchWithoutContinueOnError(@TempDir Path tempDir)
        throws Exception {
      String badTypeTemplate = DEFAULT_TEMPLATE.replace(
          "com.streamx.blueprints.page.published.v1", "bad.type");

      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withFiles(3)
          .withSubDirectory("bad", sub -> sub
              .withEventTemplate(badTypeTemplate)
              .withFiles(3))
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(), "--batch-size", "3");

      result.assertExitCode(1);
      assertThat(result.stderr()).contains("bad.type");
    }

    @Test
    void shouldContinueAfterFailedBatchWhenContinueOnErrorSet(@TempDir Path tempDir)
        throws Exception {
      String badTypeTemplate = DEFAULT_TEMPLATE.replace(
          "com.streamx.blueprints.page.published.v1", "bad.type");

      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withFiles(3)
          .withSubDirectory("bad", sub -> sub
              .withEventTemplate(badTypeTemplate)
              .withFiles(3))
          .withSubDirectory("good", sub -> sub
              .withFiles(3))
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(),
          "--batch-size", "3", "--continue-on-error");

      result.assertExitCode(0);
      assertEventsPublished(6);
      assertThat(result.stderr()).contains("bad.type");
      assertThat(result.stdout()).contains(
          msg.streamBatchPublishingCompleted(9, 6, 0, 3, 3, 2, 1));
    }
  }

  @Nested
  class ErrorHandling {

    @Test
    void shouldFailOnFirstFailedEvent(@TempDir Path tempDir) throws Exception {
      String badTypeTemplate = DEFAULT_TEMPLATE.replace(
          "com.streamx.blueprints.page.published.v1", "bad.type");

      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withFiles("valid-1.json")
          .withSubDirectory("bad", sub -> sub
              .withEventTemplate(badTypeTemplate)
              .withFiles("bad-1.json", "bad-2.json"))
          .withSubDirectory("good", sub -> sub
              .withFiles("valid-2.json", "valid-3.json"))
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString());

      result.assertExitCode(1);
      assertThat(result.stderr()).contains("bad.type");
    }

    @Test
    void shouldContinueOnInvalidEventsIfContinueOnErrorFlagProvided(@TempDir Path tempDir)
        throws Exception {
      String badTypeTemplate = DEFAULT_TEMPLATE.replace(
          "com.streamx.blueprints.page.published.v1", "bad.type");

      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withFiles("valid-1.json")
          .withSubDirectory("bad", sub -> sub
              .withEventTemplate(badTypeTemplate)
              .withFiles("bad-1.json", "bad-2.json"))
          .withSubDirectory("good", sub -> sub
              .withFiles("valid-2.json", "valid-3.json"))
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(), "--continue-on-error");

      result.assertExitCode(0);
      assertEventsPublished(3);
      assertThat(result.stderr()).contains("bad.type");
      assertThat(result.stdout()).contains(msg.eventsPublished(3));
    }

    @Test
    void shouldReturnExitCode0WhenAllEventsSucceed(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withFiles(5)
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString());

      result.assertExitCode(0);
    }

    @Test
    void shouldReturnExitCode1WhenSomeEventsFailed(@TempDir Path tempDir) throws Exception {
      String badTypeTemplate = DEFAULT_TEMPLATE.replace(
          "com.streamx.blueprints.page.published.v1", "bad.type");

      TestDirectoryGenerator.root()
          .withEventTemplate(badTypeTemplate)
          .withFiles(1)
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString());

      result.assertExitCode(1);
    }
  }

  @Nested
  class LargeScale {

    @Test
    void shouldPublish500FilesAcrossNestedDirectories(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(DEFAULT_TEMPLATE)
          .withFiles(100)
          .withSubDirectory("a", sub -> sub.withFiles(100))
          .withSubDirectory("b", sub -> sub.withFiles(100))
          .withSubDirectory("c", sub -> sub.withFiles(100))
          .withSubDirectory("d", sub -> sub.withFiles(100))
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(), "--batch-size", "50");

      result.assertSuccess();
      assertEventsPublished(500);
      assertThat(result.stdout()).contains(
          msg.streamBatchPublishingCompleted(500, 500, 0, 0, 10, 10, 0));
    }
  }
}