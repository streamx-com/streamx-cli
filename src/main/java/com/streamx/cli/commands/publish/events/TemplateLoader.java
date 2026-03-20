package com.streamx.cli.commands.publish.events;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.streamx.cli.framework.CliException;
import java.io.IOException;
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

    JsonNode patchNode;
    try {
      patchNode = mapper.readTree(patchFile.toFile());
    } catch (IOException e) {
      throw new CliException(msg.eventTemplateCorrupted(rootPath.toString()), e);
    }

    try {
      JsonPatch patch = JsonPatch.fromJson(patchNode);
      return patch.apply(template);
    } catch (JsonPatchException | IOException e) {
      throw new CliException(msg.patchIsInvalid(patchName), e);
    }
  }
}