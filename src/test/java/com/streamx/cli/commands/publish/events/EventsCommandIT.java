package com.streamx.cli.commands.publish.events;

import static com.streamx.cli.commands.publish.events.TemplateLoader.EVENTTEMPLATE_FILE;
import static com.streamx.cli.i18n.MessageProvider.msg;
import static com.streamx.cli.test.MeshAssertions.assertEventsPublished;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.test.CliBaseIT;
import com.streamx.cli.test.annotation.DisabledIfDockerUnavailable;
import com.streamx.cli.test.profiles.DefaultMeshTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
@DisabledIfDockerUnavailable
@TestProfile(DefaultMeshTestProfile.class)
public class EventsCommandIT extends CliBaseIT {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String TEMPLATE_OUTPUT_FILE = EVENTTEMPLATE_FILE + ".json";

  /** Matches the output-dir path printed by both --dry-run and --debug. */
  private static final Pattern OUTPUT_DIR_PATTERN =
      Pattern.compile("Inspect rendered events in: (.+)$", Pattern.MULTILINE);

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

  private static Path parseOutputDir(ProcessResult result) {
    Matcher m = OUTPUT_DIR_PATTERN.matcher(result.stderr());
    assertThat(m.find())
        .as("Expected 'Inspect rendered events in: <path>' in stderr:\n" + result.stderr())
        .isTrue();
    return Path.of(m.group(1).strip());
  }

  private static JsonNode readJson(Path file) throws Exception {
    assertThat(file).as("Expected artefact file to exist: " + file).exists();
    return MAPPER.readTree(file.toFile());
  }

  private static JsonNode readEvent(Path outputDir, String relativeArtefactPath) throws Exception {
    return readJson(outputDir.resolve(relativeArtefactPath)).path("event");
  }

  private static List<String> collectEventField(Path outputDir, String field) throws Exception {
    List<String> values = new ArrayList<>();
    try (var stream = java.nio.file.Files.walk(outputDir)) {
      stream
          .filter(p -> p.getFileName().toString().endsWith(".json"))
          .filter(p -> !p.getFileName().toString().equals(TEMPLATE_OUTPUT_FILE))
          .sorted()
          .forEach(p -> {
            try {
              String v = MAPPER.readTree(p.toFile()).path("event").path(field).asText();
              values.add(v);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
    }
    return values;
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
      ProcessResult result = exec("publish", "events", rootDir.toString(), "--debug");

      result.assertSuccess();
      assertEventsPublished(5);

      Path outputDir = parseOutputDir(result);
      List<String> subjects = collectEventField(outputDir, "subject");

      assertThat(subjects).containsExactlyInAnyOrder(
          rootDir.toString(),
          rootDir.resolve("sub").toString(),
          rootDir.resolve("sub-with-template").toString(),
          rootDir.resolve("sub-with-template/nested").toString(),
          rootDir.resolve("sub-with-template/nested/deeper").toString()
      );
    }

    @Test
    void shouldOverrideTemplateInSubDirectory() throws Exception {
      ProcessResult result = exec("publish", "events", rootDir.toString(), "--debug");

      result.assertSuccess();
      assertEventsPublished(5);

      Path outputDir = parseOutputDir(result);
      List<String> types = collectEventField(outputDir, "type");

      assertThat(types).anyMatch(t -> t.contains("data.published.v1"));
    }

    @Test
    void shouldResolvePayloadPathPlaceholder(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(PAYLOAD_PATH_TEMPLATE)
          .withFiles("entry.json")
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString(), "--debug");

      result.assertSuccess();
      assertEventsPublished(1);

      Path outputDir = parseOutputDir(result);
      JsonNode payload = readJson(outputDir.resolve("entry.json.json"));
      assertThat(payload.path("source").asText())
          .isEqualTo(tempDir.resolve("entry.json").toString());
    }

    @Test
    void shouldProcessDeeplyNestedDirectories(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withSubDirectory("level1", l1 -> l1
              .withFiles(1)
              .withSubDirectory("level2", l2 -> l2
                  .withFiles(1)
                  .withSubDirectory("level3", l3 -> l3
                      .withFiles(1))))
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString(), "--debug");

      result.assertSuccess();
      assertEventsPublished(3);

      Path outputDir = parseOutputDir(result);
      List<String> subjects = collectEventField(outputDir, "subject");

      assertThat(subjects).containsExactlyInAnyOrder(
          tempDir.resolve("level1").toString(),
          tempDir.resolve("level1/level2").toString(),
          tempDir.resolve("level1/level2/level3").toString()
      );
    }
  }

  @Nested
  class PatchSupport {

    @Test
    void shouldApplyPatchWhenFileIsPresent() throws Exception {
      ProcessResult result = exec(
          "publish", "events", rootDir.toString(), "--patch", "good", "--debug");

      result.assertSuccess();
      assertEventsPublished(5);

      Path outputDir = parseOutputDir(result);
      List<String> subjects = collectEventField(outputDir, "subject");

      assertThat(subjects).anyMatch(s -> s.equals("Patched subject"));
    }

    @Test
    void shouldNotApplyPatchToSubDirectoryWithOwnTemplateOrItsDescendants() throws Exception {
      ProcessResult result = exec(
          "publish", "events", rootDir.toString(), "--patch", "good", "--debug");

      result.assertSuccess();

      Path outputDir = parseOutputDir(result);
      List<String> allSubjects = collectEventField(outputDir, "subject");

      long patchedCount = allSubjects.stream()
          .filter(s -> s.equals("Patched subject"))
          .count();
      assertThat(patchedCount).isEqualTo(2);
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
      assertThat(result.stderr()).contains("missing-patch not found inside " + rootDir);
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
      ProcessResult result = exec(
          "publish", "events", rootDir.toString(), "--batch-size", "2", "--debug");

      result.assertSuccess();
      assertEventsPublished(5);
      assertThat(result.stdout()).contains(
          msg.streamBatchPublishingCompleted(5, 5, 0, 0, 3, 3, 0));

      Path outputDir = parseOutputDir(result);
      assertThat(collectEventField(outputDir, "type")).hasSize(5);
    }

    @Test
    void shouldFailOnFirstFailedBatchWithoutContinueOnError(@TempDir Path tempDir)
        throws Exception {
      String badTypeTemplate = TestDirectoryGenerator.DEFAULT_TEMPLATE.replace(
          "com.streamx.blueprints.page.published.v1", "bad.type");

      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
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
      String badTypeTemplate = TestDirectoryGenerator.DEFAULT_TEMPLATE.replace(
          "com.streamx.blueprints.page.published.v1", "bad.type");

      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withFiles(3)
          .withSubDirectory("invalid", sub -> sub
              .withEventTemplate(badTypeTemplate)
              .withFiles(3))
          .withSubDirectory("valid", sub -> sub
              .withFiles(3))
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(),
          "--batch-size", "3", "--continue-on-error", "--debug");

      result.assertExitCode(0);
      assertEventsPublished(6);
      assertThat(result.stdout()).contains(
          msg.streamBatchPublishingCompleted(9, 6, 0, 3, 3, 2, 1));

      Path outputDir = parseOutputDir(result);
      List<String> types = collectEventField(outputDir, "type");

      assertThat(types).hasSize(9);
      assertThat(types).filteredOn(t -> t.equals("com.streamx.blueprints.page.published.v1"))
          .hasSize(6);
      assertThat(types).filteredOn(t -> t.equals("bad.type"))
          .hasSize(3);
    }
  }

  @Nested
  class ErrorHandling {

    @Test
    void shouldFailOnFirstFailedEvent(@TempDir Path tempDir) throws Exception {
      String badTypeTemplate = TestDirectoryGenerator.DEFAULT_TEMPLATE.replace(
          "com.streamx.blueprints.page.published.v1", "bad.type");

      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
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
      String badTypeTemplate = TestDirectoryGenerator.DEFAULT_TEMPLATE.replace(
          "com.streamx.blueprints.page.published.v1", "bad.type");

      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withFiles("valid.json")
          .withSubDirectory("invalid", sub -> sub
              .withEventTemplate(badTypeTemplate)
              .withFiles("entry-1.json", "entry-2.json"))
          .withSubDirectory("valid", sub -> sub
              .withFiles("entry-1.json", "entry-2.json"))
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(), "--continue-on-error", "--debug");

      result.assertExitCode(0);
      assertEventsPublished(3);
      assertThat(result.stdout()).contains(msg.eventsPublished(3));

      Path outputDir = parseOutputDir(result);
      List<String> allTypes = collectEventField(outputDir, "type");

      // All 5 rendered events have artefacts (written before send).
      assertThat(allTypes).hasSize(5);
      assertThat(allTypes)
          .filteredOn(t -> t.equals("com.streamx.blueprints.page.published.v1")).hasSize(3);
      assertThat(allTypes)
          .filteredOn(t -> t.equals("bad.type")).hasSize(2);

      // The 3 successfully published events have artefact files.
      assertThat(outputDir.resolve("valid.json.json")).exists();
      assertThat(outputDir.resolve("valid/entry-1.json.json")).exists();
      assertThat(outputDir.resolve("valid/entry-2.json.json")).exists();
    }
  }

  @Nested
  class DryRun {

    @Test
    void shouldPrintOutputDirectoryPathAndNotPublish(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withFiles("page.json")
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString(), "--dry-run");

      result.assertSuccess();
      assertEventsPublished(0);
      assertThat(parseOutputDir(result)).isDirectory();
      assertThat(result.stderr()).contains("Dry-run mode");
    }

    @Test
    void shouldWriteTemplateJsonWithCorrectFields(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withFiles("page.json")
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString(), "--dry-run");

      result.assertSuccess();
      Path outputDir = parseOutputDir(result);

      JsonNode tmpl = readJson(outputDir.resolve(TEMPLATE_OUTPUT_FILE));

      assertThat(tmpl.path("source").asText())
          .isEqualTo(tempDir.resolve(EVENTTEMPLATE_FILE).toString());
      assertThat(tmpl.path("patch").isNull()).isTrue();
      assertThat(tmpl.path("template").path("type").asText())
          .isEqualTo("com.streamx.blueprints.page.published.v1");
    }

    @Test
    void shouldWritePatchPathAndPatchedTemplateInTemplateJson(@TempDir Path tempDir)
        throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withPatchFile("type", TestDirectoryGenerator.TYPE_PATCH)
          .withFiles("entry.json")
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(), "--patch", "type", "--dry-run");

      result.assertSuccess();
      Path outputDir = parseOutputDir(result);

      JsonNode tmpl = readJson(outputDir.resolve(TEMPLATE_OUTPUT_FILE));

      assertThat(tmpl.path("patch").asText())
          .isEqualTo(tempDir.resolve(".type" + EVENTTEMPLATE_FILE).toString());
      assertThat(tmpl.path("template").path("subject").asText())
          .isEqualTo("Patched subject");
    }

    @Test
    void shouldWritePayloadJsonWithCorrectFields(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withFiles("page.json")
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString(), "--dry-run");

      result.assertSuccess();
      Path outputDir = parseOutputDir(result);

      JsonNode payload = readJson(outputDir.resolve("page.json.json"));

      assertThat(payload.path("source").asText())
          .isEqualTo(tempDir.resolve("page.json").toString());
      assertThat(payload.path("templateSource").asText())
          .isEqualTo(outputDir.resolve(TEMPLATE_OUTPUT_FILE).toString());
      assertThat(payload.path("event").path("specversion").asText()).isEqualTo("1.0");
      assertThat(payload.path("event").path("type").asText())
          .isEqualTo("com.streamx.blueprints.page.published.v1");
    }

    @Test
    void shouldMirrorDirectoryStructure(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withFiles("root.json")
          .withSubDirectory("sub", sub -> sub.withFiles("child.json"))
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString(), "--dry-run");

      result.assertSuccess();
      Path outputDir = parseOutputDir(result);

      assertThat(outputDir.resolve(TEMPLATE_OUTPUT_FILE)).exists();
      assertThat(outputDir.resolve("root.json.json")).exists();
      assertThat(outputDir.resolve("sub/" + TEMPLATE_OUTPUT_FILE)).exists();
      assertThat(outputDir.resolve("sub/child.json.json")).exists();
    }

    @Test
    void shouldWriteSeparateTemplateJsonForSubDirectoryWithOwnTemplate(@TempDir Path tempDir)
        throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withFiles("root.json")
          .withSubDirectory("override", sub -> sub
              .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE.replace(
                  "com.streamx.blueprints.page.published.v1", "custom.type"))
              .withFiles("child.json"))
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString(), "--dry-run");

      result.assertSuccess();
      Path outputDir = parseOutputDir(result);

      JsonNode rootTmpl = readJson(outputDir.resolve(TEMPLATE_OUTPUT_FILE));
      JsonNode subTmpl = readJson(outputDir.resolve("override/" + TEMPLATE_OUTPUT_FILE));

      assertThat(rootTmpl.path("source").asText())
          .isEqualTo(tempDir.resolve(EVENTTEMPLATE_FILE).toString());
      assertThat(subTmpl.path("source").asText())
          .isEqualTo(tempDir.resolve("override/" + EVENTTEMPLATE_FILE).toString());
      assertThat(subTmpl.path("template").path("type").asText()).isEqualTo("custom.type");
    }

    @Test
    void shouldResolvePlaceholdersInRenderedEvent(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withFiles("entry.json")
          .withSubDirectory("sub", sub -> sub.withFiles("nested.json"))
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString(), "--dry-run");

      result.assertSuccess();
      Path outputDir = parseOutputDir(result);

      assertThat(readEvent(outputDir, "entry.json.json").path("subject").asText())
          .isEqualTo(tempDir.toString());
      assertThat(readEvent(outputDir, "sub/nested.json.json").path("subject").asText())
          .isEqualTo(tempDir.resolve("sub").toString());
    }

    @Test
    void shouldAppendJsonSuffixToPayloadFilesOfAnyExtension(@TempDir Path tempDir)
        throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(PAYLOAD_PATH_TEMPLATE)
          .withFiles("page.html", "data.xml")
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString(), "--dry-run");

      result.assertSuccess();
      Path outputDir = parseOutputDir(result);

      assertThat(outputDir.resolve("page.html.json")).exists();
      assertThat(outputDir.resolve("data.xml.json")).exists();
    }

    @Test
    void shouldLinkPayloadToCoLocatedTemplateJson(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withFiles("a.json", "b.json")
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString(), "--dry-run");

      result.assertSuccess();
      Path outputDir = parseOutputDir(result);
      String expectedTemplateSource = outputDir.resolve(TEMPLATE_OUTPUT_FILE).toString();

      assertThat(readJson(outputDir.resolve("a.json.json")).path("templateSource").asText())
          .isEqualTo(expectedTemplateSource);
      assertThat(readJson(outputDir.resolve("b.json.json")).path("templateSource").asText())
          .isEqualTo(expectedTemplateSource);
    }

    @Test
    void shouldPreferDryRunOverDebugWhenBothSpecified(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withFiles("page.json")
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(), "--dry-run", "--debug");

      result.assertSuccess();
      assertEventsPublished(0);
      assertThat(result.stderr()).contains("--dry-run takes precedence");

      Path outputDir = parseOutputDir(result);
      assertThat(outputDir.resolve("page.json.json")).exists();
    }
  }

  @Nested
  class Debug {

    @Test
    void shouldPublishEventsAndWriteArtefacts(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withFiles("page.json")
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString(), "--debug");

      result.assertSuccess();
      assertEventsPublished(1);
      assertThat(parseOutputDir(result)).isDirectory();
      assertThat(result.stderr()).contains("Debug mode");
    }

    @Test
    void shouldProduceSameArtefactStructureAsDryRun(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withFiles("page.json")
          .withSubDirectory("sub", sub -> sub.withFiles("child.json"))
          .build(tempDir);

      ProcessResult result = exec("publish", "events", tempDir.toString(), "--debug");

      result.assertSuccess();
      Path outputDir = parseOutputDir(result);

      assertThat(outputDir.resolve(TEMPLATE_OUTPUT_FILE)).exists();
      assertThat(outputDir.resolve("page.json.json")).exists();
      assertThat(outputDir.resolve("sub/" + TEMPLATE_OUTPUT_FILE)).exists();
      assertThat(outputDir.resolve("sub/child.json.json")).exists();
    }

    @Test
    void shouldApplyPatchPublishAndWriteArtefacts(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withPatchFile("type", TestDirectoryGenerator.TYPE_PATCH)
          .withFiles("entry.json")
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(), "--patch", "type", "--debug");

      result.assertSuccess();
      assertEventsPublished(1);

      Path outputDir = parseOutputDir(result);
      JsonNode tmpl = readJson(outputDir.resolve(TEMPLATE_OUTPUT_FILE));

      assertThat(tmpl.path("patch").asText())
          .isEqualTo(tempDir.resolve(".type" + EVENTTEMPLATE_FILE).toString());
      assertThat(tmpl.path("template").path("subject").asText()).isEqualTo("Patched subject");

      assertThat(readEvent(outputDir, "entry.json.json").path("subject").asText())
          .isEqualTo("Patched subject");
    }

    @Test
    void shouldPublishInBatchesAndWriteArtefacts(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withFiles("a.json", "b.json", "c.json")
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(), "--debug", "--batch-size", "2");

      result.assertSuccess();
      assertEventsPublished(3);

      Path outputDir = parseOutputDir(result);

      assertThat(outputDir.resolve("a.json.json")).exists();
      assertThat(outputDir.resolve("b.json.json")).exists();
      assertThat(outputDir.resolve("c.json.json")).exists();
    }
  }

  @Nested
  class LargeScale {

    @Test
    void shouldPublish5000FilesAcrossNestedDirectories(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
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

    @Test
    void shouldPublish5000FilesWithPayloadPathTemplate(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.PAYLOAD_PATH_TEMPLATE)
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

    @Test
    void shouldPublish5000FilesWithPatchApplied(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withPatchFile("type", TestDirectoryGenerator.TYPE_PATCH)
          .withFiles(1000)
          .withSubDirectory("a", sub -> sub.withFiles(1000))
          .withSubDirectory("b", sub -> sub.withFiles(1000))
          .withSubDirectory("c", sub -> sub.withFiles(1000))
          .withSubDirectory("d", sub -> sub.withFiles(1000))
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(), "--patch", "type", "--batch-size", "50");

      result.assertSuccess();
      assertEventsPublished(5000);
      assertThat(result.stdout()).contains(
          msg.streamBatchPublishingCompleted(5000, 5000, 0, 0, 100, 100, 0));
    }

    @Test
    void shouldFailFastOnInvalidPatchWithLargePayload(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withPatchFile("broken", TestDirectoryGenerator.INVALID_PATCH)
          .withFiles(1000)
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(), "--patch", "broken", "--batch-size", "50");

      result.assertExitCode(1);
      assertThat(result.stderr()).contains(msg.patchIsInvalid("broken"));
      assertEventsPublished(0);
    }

    @Test
    void shouldFailFastOnCorruptedSubDirectoryTemplate(@TempDir Path tempDir) throws Exception {
      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withFiles(1000)
          .withSubDirectory("corrupted", sub -> sub
              .withEventTemplate(TestDirectoryGenerator.INVALID_TEMPLATE)
              .withFiles(1000))
          .withSubDirectory("valid", sub -> sub.withFiles(1000))
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(), "--batch-size", "50");

      result.assertExitCode(1);
      assertThat(result.stderr()).contains(msg.eventTemplateCorrupted(
          tempDir.resolve("corrupted").toString()));
    }

    @Test
    void shouldContinueAndPublishValidEventsWhenSomeBatchesFail(@TempDir Path tempDir)
        throws Exception {
      String badTypeTemplate = TestDirectoryGenerator.DEFAULT_TEMPLATE.replace(
          "com.streamx.blueprints.page.published.v1", "bad.type");

      TestDirectoryGenerator.root()
          .withEventTemplate(TestDirectoryGenerator.DEFAULT_TEMPLATE)
          .withFiles(1000)
          .withSubDirectory("invalid", sub -> sub
              .withEventTemplate(badTypeTemplate)
              .withFiles(1000))
          .withSubDirectory("valid", sub -> sub.withFiles(1000))
          .build(tempDir);

      ProcessResult result = exec(
          "publish", "events", tempDir.toString(),
          "--batch-size", "50", "--continue-on-error");

      result.assertExitCode(0);
      assertEventsPublished(2000);
      assertThat(result.stdout()).contains(
          msg.streamBatchPublishingCompleted(3000, 2000, 0, 1000, 60, 40, 20));
    }
  }
}