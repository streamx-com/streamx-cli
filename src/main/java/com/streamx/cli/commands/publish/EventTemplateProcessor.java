package com.streamx.cli.commands.publish;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.ingestion.CloudEventsSerde;
import com.streamx.cli.util.FileUtils;
import io.cloudevents.CloudEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventTemplateProcessor {
  private static final Pattern RELATIVE_PATH_PLACEHOLDER_PATTERN =
      Pattern.compile("\\$\\{relativePath(?::(-?\\d+))?}");
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String eventTemplate;
  private final Path eventPayloadPath;
  private final Path templateFilePath;
  private final String subject;

  public EventTemplateProcessor(String eventTemplate, Path eventPayloadPath, String subject) {
    this(eventTemplate, eventPayloadPath, eventPayloadPath, subject);
  }

  public EventTemplateProcessor(String eventTemplate, Path eventPayloadPath,
      Path templateFilePath, String subject) {
    this.eventTemplate = eventTemplate;
    this.eventPayloadPath = eventPayloadPath;
    this.templateFilePath = templateFilePath;
    this.subject = subject;
  }

  public CloudEvent toCloudEvent() throws CliException {
    try {
      JsonNode templateJson = MAPPER.readTree(eventTemplate);
      JsonNode processed = processNode(templateJson);
      return CloudEventsSerde.fromJson(processed);
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
    if (value.equals(EventTemplatePlaceholders.PAYLOAD_CONTENT_JSON)) {
      byte[] fileBytes = Files.readAllBytes(eventPayloadPath);
      return MAPPER.readTree(fileBytes);
    }

    if (value.contains(EventTemplatePlaceholders.PAYLOAD_CONTENT_BASE64)) {
      byte[] fileBytes = Files.readAllBytes(eventPayloadPath);
      String payloadBase64 = Base64.getEncoder().encodeToString(fileBytes);
      value = value.replace(EventTemplatePlaceholders.PAYLOAD_CONTENT_BASE64, payloadBase64);
    }

    if (value.contains(EventTemplatePlaceholders.PAYLOAD_PATH)) {
      value = value.replace(EventTemplatePlaceholders.PAYLOAD_PATH, eventPayloadPath.toString());
    }

    Matcher matcher = RELATIVE_PATH_PLACEHOLDER_PATTERN.matcher(value);
    StringBuilder result = new StringBuilder();
    boolean found = false;
    while (matcher.find()) {
      found = true;
      String levelStr = matcher.group(1);
      int level = levelStr != null ? Integer.parseInt(levelStr) : 0;
      Path resolved = FileUtils.getNthParent(templateFilePath, level).relativize(eventPayloadPath);
      matcher.appendReplacement(result, Matcher.quoteReplacement(resolved.toString()));
    }
    if (found) {
      matcher.appendTail(result);
      value = result.toString();
    }

    if (value.contains(EventTemplatePlaceholders.SUBJECT)) {
      String resolvedSubject = subject != null ? subject : eventPayloadPath.toString();
      value = value.replace(EventTemplatePlaceholders.SUBJECT, resolvedSubject);
    }

    if (value.contains(EventTemplatePlaceholders.UUID)) {
      value = value.replace(EventTemplatePlaceholders.UUID, UUID.randomUUID().toString());
    }

    if (value.contains(EventTemplatePlaceholders.CURRENT_TIME)) {
      String timestamp =
          OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
      value = value.replace(EventTemplatePlaceholders.CURRENT_TIME, timestamp);
    }

    return new TextNode(value);
  }
}