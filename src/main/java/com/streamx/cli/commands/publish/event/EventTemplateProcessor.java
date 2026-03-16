package com.streamx.cli.commands.publish.event;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.ingestion.CloudEventsSerde;
import com.streamx.cli.util.FileUtils;
import io.cloudevents.CloudEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class EventTemplateProcessor {
  private static final Pattern RELATIVE_PATH_PLACEHOLDER_PATTERN =
      Pattern.compile("\\$\\{relativePath(?::(-?\\d+))?}");
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String eventTemplate;
  private final Path eventPayloadPath;
  private final String subject;

  EventTemplateProcessor(String eventTemplate, Path eventPayloadPath, String subject) {
    this.eventTemplate = eventTemplate;
    this.eventPayloadPath = eventPayloadPath;
    this.subject = subject;
  }

  CloudEvent toCloudEvent() throws CliException {
    try {
      JsonNode templateJson = MAPPER.readTree(eventTemplate);
      JsonNode processed = processNode(templateJson);
      return CloudEventsSerde.fromJson(processed);
    } catch (CliException e) {
      throw e;
    } catch (Exception e) {
      throw new CliException(msg.failedToProcessEventTemplatePlaceholders(e.getMessage()), e);
    }
  }

  private JsonNode processNode(JsonNode node) throws IOException {
    if (node.isObject()) {
      ObjectNode newObject = JsonNodeFactory.instance.objectNode();
      for (Map.Entry<String, JsonNode> property : node.properties()) {
        newObject.set(property.getKey(), processNode(property.getValue()));
      }
      return newObject;
    }

    if (node.isArray()) {
      ArrayNode newArray = JsonNodeFactory.instance.arrayNode();
      for (JsonNode element : node) {
        newArray.add(processNode(element));
      }
      return newArray;
    }

    if (node.isTextual()) {
      return processTextNode(node.textValue());
    }

    return node.deepCopy();
  }

  private JsonNode processTextNode(String value) throws IOException {
    if (value.contains("file://${payloadPath}")) {
      byte[] fileBytes = Files.readAllBytes(eventPayloadPath);
      String payloadBase64 = Base64.getEncoder().encodeToString(fileBytes);
      String resolved = value.replace("file://${payloadPath}", payloadBase64);
      return new TextNode(resolved);
    }

    if (value.contains("${payloadPath}")) {
      String resolved = value.replace("${payloadPath}", eventPayloadPath.toString());
      return new TextNode(resolved);
    }

    Matcher matcher = RELATIVE_PATH_PLACEHOLDER_PATTERN.matcher(value);
    StringBuilder result = new StringBuilder();

    boolean found = false;
    while (matcher.find()) {
      found = true;
      String levelStr = matcher.group(1);
      int level = (levelStr != null) ? Integer.parseInt(levelStr) : 0;
      Path resolved = FileUtils.getNthParent(eventPayloadPath, level);
      matcher.appendReplacement(result, Matcher.quoteReplacement(resolved.toString()));
    }
    if (found) {
      matcher.appendTail(result);
      return new TextNode(result.toString());
    }

    if (value.contains("${subject}")) {
      String resolvedSubject = subject != null ? subject : eventPayloadPath.toString();
      String resolved = value.replace("${subject}", resolvedSubject);
      return new TextNode(resolved);
    }

    return new TextNode(value);
  }
}