package com.streamx.cli.commands.publish.events;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TestDirectoryGenerator {

  public static final String DEFAULT_TEMPLATE = """
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

  public static final String PAYLOAD_PATH_TEMPLATE = """
      {
        "specversion": "1.0",
        "id": "test-id",
        "source": "streamx-test",
        "type": "com.streamx.blueprints.page.published.v1",
        "datacontenttype": "application/json",
        "subject": "Some subject",
        "time": "2026-01-01T00:00:00.000000Z",
        "data": { "content": "file://${payloadPath}" }
      }
      """;

  public static final String TYPE_PATCH = """
      [
        {
          "op": "replace",
          "path": "/subject",
          "value": "Patched subject"
        },
        {
          "op": "replace",
          "path": "/data/content",
          "value": "Patched content"
        }
      ]
      """;

  public static final String INVALID_TEMPLATE = "{ this is not valid template !!";

  public static final String INVALID_PATCH = "[ { \"op\": \"invalid-op\" } ]";

  private static class DirNode {

    String eventTemplate = null;
    final Map<String, String> patchFiles = new LinkedHashMap<>();
    final List<String> fileNames = new ArrayList<>();
    int generatedFileCount = 0;
    String generatedFilePrefix = "payload";
    String generatedFileExtension = ".json";
    String generatedFileContent = "{\"auto\":true}";
    final Map<String, DirNode> subDirs = new LinkedHashMap<>();
  }

  public static Builder root() {
    return new Builder(new DirNode());
  }

  public static final class Builder {

    private final DirNode node;

    private Builder(DirNode node) {
      this.node = node;
    }

    public Builder withEventTemplate(String json) {
      node.eventTemplate = json;
      return this;
    }

    public Builder withFiles(int count) {
      node.generatedFileCount = count;
      return this;
    }

    public Builder withFiles(int count, String prefix, String extension) {
      node.generatedFileCount = count;
      node.generatedFilePrefix = prefix;
      node.generatedFileExtension = extension;
      return this;
    }

    public Builder withFiles(int count, String fileContent) {
      node.generatedFileCount = count;
      node.generatedFileContent = fileContent;
      return this;
    }

    public Builder withFiles(String... names) {
      node.fileNames.addAll(List.of(names));
      return this;
    }

    public Builder withPatchFile(String patchName, String json) {
      node.patchFiles.put(patchName, json);
      return this;
    }

    public Builder withSubDirectory(String name, Consumer<Builder> configure) {
      DirNode child = new DirNode();
      Builder childBuilder = new Builder(child);
      configure.accept(childBuilder);
      node.subDirs.put(name, child);
      return this;
    }

    public void build(Path parentDir) throws IOException {
      materialize(parentDir, node);
      System.out.println("Events test directory: " + parentDir.toAbsolutePath());
    }
  }

  private static void materialize(Path dir, DirNode node) throws IOException {
    Files.createDirectories(dir);

    if (node.eventTemplate != null) {
      Files.writeString(dir.resolve(".eventtemplate"), node.eventTemplate);
    }

    for (Map.Entry<String, String> e : node.patchFiles.entrySet()) {
      String fileName = "." + e.getKey() + ".eventtemplate";
      Files.writeString(dir.resolve(fileName), e.getValue());
    }

    for (String name : node.fileNames) {
      Files.writeString(dir.resolve(name), "{\"auto\":true}");
    }

    for (int i = 0; i < node.generatedFileCount; i++) {
      String name = node.generatedFilePrefix + "-" + i + node.generatedFileExtension;
      Files.writeString(dir.resolve(name), node.generatedFileContent);
    }

    for (Map.Entry<String, DirNode> e : node.subDirs.entrySet()) {
      materialize(dir.resolve(e.getKey()), e.getValue());
    }
  }
}