package com.streamx.cli.commands.publish.events;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static com.streamx.cli.test.MeshAssertions.assertEventsPublished;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.annotation.DisabledIfDockerUnavailable;
import com.streamx.cli.test.profiles.DefaultMeshTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
@DisabledIfDockerUnavailable
@TestProfile(DefaultMeshTestProfile.class)
public class EventsCommandIT extends CliBaseIT {

  private static final String PAGE_TEMPLATE = """
      {
        "specversion": "1.0",
        "id": "test-id",
        "source": "streamx-test",
        "type": "com.streamx.blueprints.page.published.v1",
        "datacontenttype": "application/json",
        "subject": "${relativePath}",
        "time": "2026-01-01T00:00:00.000000Z",
        "data": { "content": "test" }
      }
      """;

  private static final String PAYLOAD_PATH_TEMPLATE = """
      {
        "specversion": "1.0",
        "id": "test-id",
        "source": "streamx-test",
        "type": "com.streamx.blueprints.page.published.v1",
        "datacontenttype": "application/json",
        "subject": "entry",
        "time": "2026-01-01T00:00:00.000000Z",
        "data": { "content": "file://${payloadPath}" }
      }
      """;

  private static final String INVALID_TEMPLATE = "{ this is not valid template !!";

  static Path rootDir;

  @BeforeAll
  static void resolveStructure() throws URISyntaxException {
    rootDir = Paths.get(
        EventsCommandIT.class.getResource("/commands/publish/events/test").toURI());
  }

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
    void shouldPublishAllPayloadFiles() throws Exception {
      ProcessResult result = exec("publish", "events", rootDir.toString());

      result.assertSuccess();
      assertEventsPublished(5);
      assertThat(result.stdout()).contains(msg.eventsPublished(5));
    }

    @Test
    void shouldNotPublishTemplateOrPatchFilesAsPayloads() throws Exception {
      ProcessResult result = exec("publish", "events", rootDir.toString());

      result.assertSuccess();
      assertEventsPublished(5);
    }

    @Test
    void shouldResolveRelativePathPlaceholder() throws Exception {
      ProcessResult result = exec("publish", "events", rootDir.toString());

      result.assertSuccess();
      assertThat(result.stderr()).contains("subject='" + rootDir + "'");
      assertThat(result.stderr()).contains("subject='" + rootDir.resolve("sub") + "'");
      assertThat(result.stderr())
          .contains("subject='" + rootDir.resolve("sub-with-template") + "'");
      assertThat(result.stderr())
          .contains("subject='" + rootDir.resolve("sub-with-template/nested") + "'");
      assertThat(result.stderr())
          .contains("subject='" + rootDir.resolve("sub-with-template/nested/deeper") + "'");
    }

    @Test
    void shouldResolvePayloadPathPlaceholder(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(PAYLOAD_PATH_TEMPLATE)
          .withFiles("entry.json")
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString());

      result.assertSuccess();
      assertEventsPublished(1);
    }

    @Test
    void shouldOverrideTemplateInSubDirectory() throws Exception {
      ProcessResult result = exec("publish", "events", rootDir.toString());

      result.assertSuccess();
      assertThat(result.stderr()).contains("data.published.v1");
    }

    @Test
    void shouldProcessDeeplyNestedDirectories(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(PAGE_TEMPLATE)
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
  class PatchSupport {

    @Test
    void shouldApplyPatchWhenFileIsPresent() throws Exception {
      ProcessResult result = exec("publish", "events", rootDir.toString(), "--patch", "good");

      result.assertSuccess();
      assertEventsPublished(5);
      assertThat(result.stderr()).contains("Patched subject");
    }

    @Test
    void shouldNotApplyPatchToSubDirectoryWithOwnTemplateOrItsDescendants() throws Exception {
      ProcessResult result = exec("publish", "events", rootDir.toString(), "--patch", "good");

      result.assertSuccess();

      assertThat(result.stderr())
          .contains("subject='" + rootDir.resolve("sub-with-template") + "'");
      assertThat(result.stderr())
          .contains("subject='" + rootDir.resolve("sub-with-template/nested") + "'");
      assertThat(result.stderr())
          .contains("subject='" + rootDir.resolve("sub-with-template/nested/deeper") + "'");
    }

    @Test
    void shouldFailWhenPatchIsInvalid() throws Exception {
      ProcessResult result = exec("publish", "events", rootDir.toString(), "--patch", "bad");

      result.assertExitCode(1);
      assertThat(result.stderr()).contains(msg.patchIsInvalid("bad"));
      assertEventsPublished(0);
    }

    @Test
    void shouldContinueWithoutPatchWhenUserConfirms() throws Exception {
      ProcessResult result = execWithStdin(
          "y\n",
          "publish", "events", rootDir.toString(), "--patch", "missing-patch");

      result.assertSuccess();
      assertEventsPublished(5);
      assertThat(result.stderr()).contains(msg.patchNotFound("missing-patch", rootDir.toString()));
    }

    @Test
    void shouldAbortWhenMissingPatchAndUserDeclines() throws Exception {
      ProcessResult result = execWithStdin(
          "n\n",
          "publish", "events", rootDir.toString(), "--patch", "missing-patch");

      result.assertExitCode(0);
      assertEventsPublished(0);
    }
  }

  @Nested
  class BatchPublishing {

    @Test
    void shouldPublishInBatches() throws Exception {
      // 5 events, batch-size 2 → batches of 2, 2, 1
      ProcessResult result = exec(
          "publish", "events", rootDir.toString(), "--batch-size", "2");

      result.assertSuccess();
      assertEventsPublished(5);
      assertThat(result.stdout()).contains(
          msg.streamBatchPublishingCompleted(5, 5, 0, 0, 3, 3, 0));
    }

    @Test
    void shouldFailOnFirstFailedBatchWithoutContinueOnError(@TempDir Path tempDir)
        throws Exception {
      String badTypeTemplate = PAGE_TEMPLATE.replace(
          "com.streamx.blueprints.page.published.v1", "bad.type");

      TestDirectoryGenerator.root()
          .withEventTemplate(PAGE_TEMPLATE)
          .withFiles(3)
          .withSubDirectory("invalid", sub -> sub
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
      String badTypeTemplate = PAGE_TEMPLATE.replace(
          "com.streamx.blueprints.page.published.v1", "bad.type");

      TestDirectoryGenerator.root()
          .withEventTemplate(PAGE_TEMPLATE)
          .withFiles(3)
          .withSubDirectory("invalid", sub -> sub
              .withEventTemplate(badTypeTemplate)
              .withFiles(3))
          .withSubDirectory("valid", sub -> sub
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
      String badTypeTemplate = PAGE_TEMPLATE.replace(
          "com.streamx.blueprints.page.published.v1", "bad.type");

      TestDirectoryGenerator.root()
          .withEventTemplate(PAGE_TEMPLATE)
          .withFiles("valid.json")
          .withSubDirectory("invalid", sub -> sub
              .withEventTemplate(badTypeTemplate)
              .withFiles("entry.json"))
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString());

      result.assertExitCode(1);
      assertThat(result.stderr()).contains("bad.type");
    }

    @Test
    void shouldContinueOnFailedEventsWhenContinueOnErrorSet(@TempDir Path tempDir)
        throws Exception {
      String badTypeTemplate = PAGE_TEMPLATE.replace(
          "com.streamx.blueprints.page.published.v1", "bad.type");

      TestDirectoryGenerator.root()
          .withEventTemplate(PAGE_TEMPLATE)
          .withFiles("valid.json")
          .withSubDirectory("invalid", sub -> sub
              .withEventTemplate(badTypeTemplate)
              .withFiles("entry-1.json", "entry-2.json"))
          .withSubDirectory("valid", sub -> sub
              .withFiles("entry-1.json", "entry-2.json"))
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(), "--continue-on-error");

      result.assertExitCode(0);
      assertEventsPublished(3);
      assertThat(result.stderr()).contains("bad.type");
      assertThat(result.stdout()).contains(msg.eventsPublished(3));
    }
  }

  @Nested
  class LargeScale {

    @Test
    void shouldPublish5000FilesAcrossNestedDirectories(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(PAGE_TEMPLATE)
          .withFiles(1000)
          .withSubDirectory("a", sub -> sub.withFiles(1000))
          .withSubDirectory("b", sub -> sub.withFiles(1000))
          .withSubDirectory("c", sub -> sub.withFiles(1000))
          .withSubDirectory("d", sub -> sub.withFiles(1000))
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(), "--batch-size", "50");

      result.assertSuccess();
      assertEventsPublished(5000);
      assertThat(result.stdout()).contains(
          msg.streamBatchPublishingCompleted(5000, 5000, 0, 0, 100, 100, 0));
    }
  }
}