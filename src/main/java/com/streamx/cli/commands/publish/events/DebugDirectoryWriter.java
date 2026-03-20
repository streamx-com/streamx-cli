package com.streamx.cli.commands.publish.events;

import static com.streamx.cli.commands.publish.events.TemplateLoader.EVENTTEMPLATE_FILE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

class DebugDirectoryWriter {

  static final String DATE_FORMAT_PATTERN = "yyyyMMdd";
  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);

  static final String TEMPLATE_OUTPUT_FILE = EVENTTEMPLATE_FILE + ".json";

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .enable(SerializationFeature.INDENT_OUTPUT);

  private final Path tempDir;
  private final Path sourceRoot;

  private final Set<Path> templatesWritten = new HashSet<>();

  DebugDirectoryWriter(Path sourceRoot) throws IOException {
    String prefix = "streamx-cli-" + LocalDate.now().format(DATE_FORMAT) + "-";
    this.tempDir = Files.createTempDirectory(prefix);
    this.sourceRoot = sourceRoot;
  }

  Path getTempDir() {
    return tempDir;
  }

  void writeEvent(Path payloadPath, TemplateContext ctx, JsonNode renderedEvent)
      throws IOException {
    Path outputDir = resolveOutputDir(payloadPath.getParent());
    Files.createDirectories(outputDir);

    Path templateOutputPath = outputDir.resolve(TEMPLATE_OUTPUT_FILE);
    writeTemplateOnce(templateOutputPath, ctx);
    writePayloadFile(outputDir, payloadPath, templateOutputPath, renderedEvent);
  }

  private Path resolveOutputDir(Path sourceDir) {
    Path relative = sourceRoot.relativize(sourceDir);
    return relative.toString().isEmpty() ? tempDir : tempDir.resolve(relative);
  }

  private void writeTemplateOnce(Path dest, TemplateContext ctx) throws IOException {
    if (templatesWritten.contains(dest)) {
      return;
    }
    ObjectNode node = MAPPER.createObjectNode();
    node.put("source", ctx.templatePath());
    node.put("patch", ctx.patchPath());        // null → JSON null
    node.set("template", ctx.template());
    MAPPER.writeValue(dest.toFile(), node);
    templatesWritten.add(dest);
  }

  private void writePayloadFile(
      Path outputDir, Path payloadPath, Path templateOutputPath, JsonNode renderedEvent)
      throws IOException {
    String outputFileName = payloadPath.getFileName().toString() + ".json";

    ObjectNode node = MAPPER.createObjectNode();
    node.put("source", payloadPath.toString());
    node.put("templateSource", templateOutputPath.toString());
    node.set("event", renderedEvent);

    Path dest = outputDir.resolve(outputFileName);
    MAPPER.writeValue(dest.toFile(), node);
  }
}