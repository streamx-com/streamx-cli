package com.streamx.cli.commands.publish.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.streamx.cli.util.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

public class TemplateResolver {
  public static String getEventTemplate(String eventType) {
    return """
        {
          "specversion": "1.0",
          "id": "Accent Furniture",
          "source": "streamx-commerce-accelerator",
          "type": "com.streamx.blueprints.data.published.v1",
          "datacontenttype": "application/json",
          "subject": "${relativePath}",
          "time": "2026-01-01T00:00:00.000000Z",
          "data": {
            "content": "{\\"id\\":\\"Accent Furniture\\",\\"slug\\":\\"accent-furniture\\",\\"name\\":\\"Accent Furniture\\"}",
            "type": "data/category"
          }
        }
        """;
  }

  @NotNull
  private String calculateRelativePath(Path file, EventSourceDescriptor eventSource) {
    String relativePath;
    if (eventSource.getRelativePathLevel() == null) {
      relativePath = FileUtils.toString(
          Path.of(batchIngestionArguments.getSourceDirectory()).relativize(file));
    } else {
      relativePath = FileUtils.toString(FileUtils.getNthParent(eventSource.getSource(),
          eventSource.getRelativePathLevel()).relativize(file));
    }
    return relativePath;
  }
}
