package com.streamx.cli.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.framework.CliException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConcatenatedJsonParserTest {

  private ConcatenatedJsonParser parser;

  @BeforeEach
  void setUp() {
    parser = new ConcatenatedJsonParser();
  }

  private InputStream toInputStream(String input) {
    return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
  }

  @Nested
  class ParseString {

    @Test
    void shouldParseSingleObject() {
      String input = """
          {"id": "1", "name": "foo"}
          """;

      List<JsonNode> result = parser.parse(input);

      assertEquals(1, result.size());
      assertEquals("1", result.get(0).get("id").asText());
      assertEquals("foo", result.get(0).get("name").asText());
    }

    @Test
    void shouldParseMultipleObjects() {
      String input = """
          {"id": "1", "name": "foo"}
          {"id": "2", "name": "bar"}
          {"id": "3", "name": "baz"}
          """;

      List<JsonNode> result = parser.parse(input);

      assertEquals(3, result.size());
      assertEquals("1", result.get(0).get("id").asText());
      assertEquals("2", result.get(1).get("id").asText());
      assertEquals("3", result.get(2).get("id").asText());
    }

    @Test
    void shouldParseMultipleObjectsNoWhitespace() {
      String input = """
          {"id": "1"}{"id": "2"}{"id": "3"}""";

      List<JsonNode> result = parser.parse(input);

      assertEquals(3, result.size());
    }

    @Test
    void shouldParseNestedObjects() {
      String input = """
          {"id": "1", "data": {"content": "hello", "type": "text"}}
          {"id": "2", "data": {"content": "world", "type": "text"}}
          """;

      List<JsonNode> result = parser.parse(input);

      assertEquals(2, result.size());
      assertEquals("hello", result.get(0).get("data").get("content").asText());
      assertEquals("world", result.get(1).get("data").get("content").asText());
    }

    @Test
    void shouldHandleEmptyInput() {
      List<JsonNode> result = parser.parse("");

      assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowOnInvalidJsonInput() {
      assertThrows(CliException.class, () ->
          parser.parse("{invalid json}"));
    }
  }

  @Nested
  class ParseInputStream {

    @Test
    void shouldParseSingleObject() {
      try (Stream<JsonNode> stream = parser.parse(
          toInputStream("{\"id\": \"1\", \"name\": \"foo\"}"))) {

        List<JsonNode> result = stream.toList();

        assertEquals(1, result.size());
        assertEquals("foo", result.get(0).get("name").asText());
      }
    }

    @Test
    void shouldParseMultipleObjects() {
      String input = "{\"id\": \"1\"}\n{\"id\": \"2\"}\n{\"id\": \"3\"}";

      try (Stream<JsonNode> stream = parser.parse(toInputStream(input))) {
        List<JsonNode> result = stream.toList();

        assertEquals(3, result.size());
        assertEquals("1", result.get(0).get("id").asText());
        assertEquals("2", result.get(1).get("id").asText());
        assertEquals("3", result.get(2).get("id").asText());
      }
    }

    @Test
    void shouldProcessStreamLazily() {
      String input = "{\"id\": \"1\"}\n{\"id\": \"2\"}\n{\"id\": \"3\"}";

      try (Stream<JsonNode> stream = parser.parse(toInputStream(input))) {
        JsonNode first = stream.findFirst().orElseThrow();

        assertEquals("1", first.get("id").asText());
      }
    }

    @Test
    void shouldHandleEmptyInput() {
      try (Stream<JsonNode> stream = parser.parse(toInputStream(""))) {
        assertEquals(0, stream.count());
      }
    }

    @Test
    void shouldThrowOnInvalidJson() {
      try (Stream<JsonNode> stream = parser.parse(
          toInputStream("{broken"))) {
        assertThrows(CliException.class, () -> stream.toList());
      }
    }
  }
}