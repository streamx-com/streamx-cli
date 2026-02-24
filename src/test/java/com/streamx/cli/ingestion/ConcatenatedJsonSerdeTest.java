package com.streamx.cli.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.framework.CliException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConcatenatedJsonSerdeTest {

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

      List<JsonNode> result = ConcatenatedJsonSerde.parse(input);

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

      List<JsonNode> result = ConcatenatedJsonSerde.parse(input);

      assertEquals(3, result.size());
      assertEquals("1", result.get(0).get("id").asText());
      assertEquals("2", result.get(1).get("id").asText());
      assertEquals("3", result.get(2).get("id").asText());
    }

    @Test
    void shouldParseMultipleObjectsNoWhitespace() {
      String input = """
          {"id": "1"}{"id": "2"}{"id": "3"}""";

      List<JsonNode> result = ConcatenatedJsonSerde.parse(input);

      assertEquals(3, result.size());
    }

    @Test
    void shouldParseNestedObjects() {
      String input = """
          {"id": "1", "data": {"content": "hello", "type": "text"}}
          {"id": "2", "data": {"content": "world", "type": "text"}}
          """;

      List<JsonNode> result = ConcatenatedJsonSerde.parse(input);

      assertEquals(2, result.size());
      assertEquals("hello", result.get(0).get("data").get("content").asText());
      assertEquals("world", result.get(1).get("data").get("content").asText());
    }

    @Test
    void shouldHandleEmptyInput() {
      List<JsonNode> result = ConcatenatedJsonSerde.parse("");

      assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowOnInvalidJsonInput() {
      assertThrows(CliException.class, () ->
          ConcatenatedJsonSerde.parse("{invalid json}"));
    }
  }

  @Nested
  class ParseInputStream {

    @Test
    void shouldParseSingleObject() {
      try (Stream<JsonNode> stream = ConcatenatedJsonSerde.parse(
          toInputStream("{\"id\": \"1\", \"name\": \"foo\"}"))) {

        List<JsonNode> result = stream.toList();

        assertEquals(1, result.size());
        assertEquals("foo", result.get(0).get("name").asText());
      }
    }

    @Test
    void shouldParseMultipleObjects() {
      String input = "{\"id\": \"1\"}\n{\"id\": \"2\"}\n{\"id\": \"3\"}";

      try (Stream<JsonNode> stream = ConcatenatedJsonSerde.parse(toInputStream(input))) {
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

      try (Stream<JsonNode> stream = ConcatenatedJsonSerde.parse(toInputStream(input))) {
        JsonNode first = stream.findFirst().orElseThrow();

        assertEquals("1", first.get("id").asText());
      }
    }

    @Test
    void shouldHandleEmptyInput() {
      try (Stream<JsonNode> stream = ConcatenatedJsonSerde.parse(toInputStream(""))) {
        assertEquals(0, stream.count());
      }
    }

    @Test
    void shouldThrowOnInvalidJson() {
      try (Stream<JsonNode> stream = ConcatenatedJsonSerde.parse(
          toInputStream("{broken"))) {
        assertThrows(CliException.class, stream::toList);
      }
    }
  }

  @Nested
  class SerializeString {

    @Test
    void shouldSerializeSingleObject() {
      List<JsonNode> nodes = ConcatenatedJsonSerde.parse("{\"id\": \"1\", \"name\": \"foo\"}");

      String result = ConcatenatedJsonSerde.serialize(nodes);

      List<JsonNode> reparsed = ConcatenatedJsonSerde.parse(result);
      assertEquals(1, reparsed.size());
      assertEquals("1", reparsed.get(0).get("id").asText());
      assertEquals("foo", reparsed.get(0).get("name").asText());
    }

    @Test
    void shouldSerializeMultipleObjects() {
      List<JsonNode> nodes = ConcatenatedJsonSerde.parse(
          "{\"id\": \"1\"}{\"id\": \"2\"}{\"id\": \"3\"}");

      String result = ConcatenatedJsonSerde.serialize(nodes);

      List<JsonNode> reparsed = ConcatenatedJsonSerde.parse(result);
      assertEquals(3, reparsed.size());
      assertEquals("1", reparsed.get(0).get("id").asText());
      assertEquals("2", reparsed.get(1).get("id").asText());
      assertEquals("3", reparsed.get(2).get("id").asText());
    }

    @Test
    void shouldSerializeNestedObjects() {
      List<JsonNode> nodes = ConcatenatedJsonSerde.parse(
          "{\"id\": \"1\", \"data\": {\"content\": \"hello\", \"type\": \"text\"}}");

      String result = ConcatenatedJsonSerde.serialize(nodes);

      List<JsonNode> reparsed = ConcatenatedJsonSerde.parse(result);
      assertEquals(1, reparsed.size());
      assertEquals("hello", reparsed.get(0).get("data").get("content").asText());
    }

    @Test
    void shouldHandleEmptyList() {
      String result = ConcatenatedJsonSerde.serialize(List.of());

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  class SerializeOutputStream {

    @Test
    void shouldSerializeSingleObject() {
      List<JsonNode> nodes = ConcatenatedJsonSerde.parse("{\"id\": \"1\", \"name\": \"foo\"}");
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      ConcatenatedJsonSerde.serialize(nodes.stream(), out);

      List<JsonNode> reparsed = ConcatenatedJsonSerde.parse(out.toString(StandardCharsets.UTF_8));
      assertEquals(1, reparsed.size());
      assertEquals("foo", reparsed.get(0).get("name").asText());
    }

    @Test
    void shouldSerializeMultipleObjects() {
      List<JsonNode> nodes = ConcatenatedJsonSerde.parse(
          "{\"id\": \"1\"}{\"id\": \"2\"}{\"id\": \"3\"}");
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      ConcatenatedJsonSerde.serialize(nodes.stream(), out);

      List<JsonNode> reparsed = ConcatenatedJsonSerde.parse(out.toString(StandardCharsets.UTF_8));
      assertEquals(3, reparsed.size());
      assertEquals("1", reparsed.get(0).get("id").asText());
      assertEquals("2", reparsed.get(1).get("id").asText());
      assertEquals("3", reparsed.get(2).get("id").asText());
    }

    @Test
    void shouldHandleEmptyStream() {
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      ConcatenatedJsonSerde.serialize(Stream.empty(), out);

      assertEquals(0, out.size());
    }
  }

  @Nested
  class RoundTrip {

    @Test
    void shouldRoundTripThroughStringMethods() {
      String original = "{\"id\":\"1\",\"name\":\"foo\"}{\"id\":\"2\",\"name\":\"bar\"}";

      List<JsonNode> parsed = ConcatenatedJsonSerde.parse(original);
      String serialized = ConcatenatedJsonSerde.serialize(parsed);
      List<JsonNode> reparsed = ConcatenatedJsonSerde.parse(serialized);

      assertEquals(parsed.size(), reparsed.size());
      for (int i = 0; i < parsed.size(); i++) {
        assertEquals(parsed.get(i), reparsed.get(i));
      }
    }

    @Test
    void shouldRoundTripThroughStreamMethods() {
      String original = "{\"id\":\"1\"}{\"id\":\"2\"}{\"id\":\"3\"}";
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      List<JsonNode> parsed = ConcatenatedJsonSerde.parse(original);
      ConcatenatedJsonSerde.serialize(parsed.stream(), out);
      String serialized = out.toString(StandardCharsets.UTF_8);
      List<JsonNode> reparsed = ConcatenatedJsonSerde.parse(serialized);

      assertEquals(parsed.size(), reparsed.size());
      for (int i = 0; i < parsed.size(); i++) {
        assertEquals(parsed.get(i), reparsed.get(i));
      }
    }
  }
}