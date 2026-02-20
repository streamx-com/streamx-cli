package com.streamx.cli.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConcatenatedJsonSerializerTest {

  private ConcatenatedJsonSerializer serializer;
  private ConcatenatedJsonParser parser;

  @BeforeEach
  void setUp() {
    serializer = new ConcatenatedJsonSerializer();
    parser = new ConcatenatedJsonParser();
  }

  @Nested
  class SerializeString {

    @Test
    void shouldSerializeSingleObject() {
      List<JsonNode> nodes = parser.parse("{\"id\": \"1\", \"name\": \"foo\"}");

      String result = serializer.serialize(nodes);

      List<JsonNode> reparsed = parser.parse(result);
      assertEquals(1, reparsed.size());
      assertEquals("1", reparsed.get(0).get("id").asText());
      assertEquals("foo", reparsed.get(0).get("name").asText());
    }

    @Test
    void shouldSerializeMultipleObjects() {
      List<JsonNode> nodes = parser.parse(
          "{\"id\": \"1\"}{\"id\": \"2\"}{\"id\": \"3\"}");

      String result = serializer.serialize(nodes);

      List<JsonNode> reparsed = parser.parse(result);
      assertEquals(3, reparsed.size());
      assertEquals("1", reparsed.get(0).get("id").asText());
      assertEquals("2", reparsed.get(1).get("id").asText());
      assertEquals("3", reparsed.get(2).get("id").asText());
    }

    @Test
    void shouldSerializeNestedObjects() {
      List<JsonNode> nodes = parser.parse(
          "{\"id\": \"1\", \"data\": {\"content\": \"hello\", \"type\": \"text\"}}");

      String result = serializer.serialize(nodes);

      List<JsonNode> reparsed = parser.parse(result);
      assertEquals(1, reparsed.size());
      assertEquals("hello", reparsed.get(0).get("data").get("content").asText());
    }

    @Test
    void shouldHandleEmptyList() {
      String result = serializer.serialize(List.of());

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  class SerializeOutputStream {

    @Test
    void shouldSerializeSingleObject() {
      List<JsonNode> nodes = parser.parse("{\"id\": \"1\", \"name\": \"foo\"}");
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      serializer.serialize(nodes.stream(), out);

      List<JsonNode> reparsed = parser.parse(out.toString(StandardCharsets.UTF_8));
      assertEquals(1, reparsed.size());
      assertEquals("foo", reparsed.get(0).get("name").asText());
    }

    @Test
    void shouldSerializeMultipleObjects() {
      List<JsonNode> nodes = parser.parse(
          "{\"id\": \"1\"}{\"id\": \"2\"}{\"id\": \"3\"}");
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      serializer.serialize(nodes.stream(), out);

      List<JsonNode> reparsed = parser.parse(out.toString(StandardCharsets.UTF_8));
      assertEquals(3, reparsed.size());
      assertEquals("1", reparsed.get(0).get("id").asText());
      assertEquals("2", reparsed.get(1).get("id").asText());
      assertEquals("3", reparsed.get(2).get("id").asText());
    }

    @Test
    void shouldHandleEmptyStream() {
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      serializer.serialize(Stream.empty(), out);

      assertEquals(0, out.size());
    }
  }

  @Nested
  class RoundTrip {

    @Test
    void shouldRoundTripThroughStringMethods() {
      String original = "{\"id\":\"1\",\"name\":\"foo\"}{\"id\":\"2\",\"name\":\"bar\"}";

      List<JsonNode> parsed = parser.parse(original);
      String serialized = serializer.serialize(parsed);
      List<JsonNode> reparsed = parser.parse(serialized);

      assertEquals(parsed.size(), reparsed.size());
      for (int i = 0; i < parsed.size(); i++) {
        assertEquals(parsed.get(i), reparsed.get(i));
      }
    }

    @Test
    void shouldRoundTripThroughStreamMethods() {
      String original = "{\"id\":\"1\"}{\"id\":\"2\"}{\"id\":\"3\"}";
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      List<JsonNode> parsed = parser.parse(original);
      serializer.serialize(parsed.stream(), out);
      String serialized = out.toString(StandardCharsets.UTF_8);
      List<JsonNode> reparsed = parser.parse(serialized);

      assertEquals(parsed.size(), reparsed.size());
      for (int i = 0; i < parsed.size(); i++) {
        assertEquals(parsed.get(i), reparsed.get(i));
      }
    }
  }
}