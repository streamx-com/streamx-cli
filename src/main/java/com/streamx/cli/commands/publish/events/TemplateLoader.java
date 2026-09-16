package com.streamx.cli.commands.publish.events;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.framework.CliException;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonPatch;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

class TemplateLoader {

  static final String EVENTTEMPLATE_FILE = ".eventtemplate";
  private static final ObjectMapper mapper = new ObjectMapper();

  static JsonNode load(Path templateFile, Path contextPath) {
    try {
      return mapper.readTree(templateFile.toFile());
    } catch (IOException e) {
      throw new CliException(msg.eventTemplateCorrupted(contextPath.toString()), e);
    }
  }

  static JsonNode applyPatch(Path rootPath, JsonNode template, String patchName,
      Supplier<Boolean> confirmContinue) {
    String patchFileName = "." + patchName + EVENTTEMPLATE_FILE;
    Path patchFile = rootPath.resolve(patchFileName);

    if (!Files.exists(patchFile)) {
      return Boolean.TRUE.equals(confirmContinue.get()) ? template : null;
    }

    try {
      JsonArray patchArray;
      try (JsonReader patchReader =
          Json.createReader(Files.newBufferedReader(patchFile))) {
        patchArray = patchReader.readArray();
      }
      JsonPatch patch = Json.createPatch(patchArray);
      JsonStructure target;
      try (JsonReader targetReader = Json.createReader(
          new StringReader(mapper.writeValueAsString(template)))) {
        target = targetReader.read();
      }
      JsonStructure result = patch.apply(target);
      return mapper.readTree(result.toString());

    } catch (Exception e) {
      throw new CliException(msg.patchIsInvalid(patchName), e);
    }
  }
}