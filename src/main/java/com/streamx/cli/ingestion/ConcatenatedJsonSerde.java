package com.streamx.cli.ingestion;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.util.JacksonUtils;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ConcatenatedJsonSerde {
  public static List<JsonNode> parse(String input) {
    ObjectMapper mapper = new ObjectMapper();
    List<JsonNode> result = new ArrayList<>();

    try (JsonParser parser = mapper.getFactory().createParser(input)) {
      Iterator<JsonNode> it = mapper.readValues(parser, JsonNode.class);
      it.forEachRemaining(result::add);
    } catch (Exception e) {
      String message = JacksonUtils.formatException(e);
      throw new CliException(msg.failedToParseJson(message), e);
    }

    return result;
  }

  public static Stream<JsonNode> parse(InputStream inputStream) {
    ObjectMapper mapper = new ObjectMapper();

    try {
      JsonParser parser = mapper.getFactory().createParser(inputStream);
      Iterator<JsonNode> iterator = mapper.readValues(parser, JsonNode.class);

      Spliterator<JsonNode> spliterator = getSpliterator(iterator);

      return StreamSupport.stream(spliterator, false)
          .onClose(() -> {
            try {
              parser.close();
            } catch (Exception e) {
              throw new CliException(msg.failedToCloseJsonParser(e.getMessage()), e);
            }
          });
    } catch (Exception e) {
      String message = JacksonUtils.formatException(e);
      throw new CliException(msg.failedToParseJson(message), e);
    }
  }

  public static String serialize(List<JsonNode> nodes) {
    ObjectMapper mapper = new ObjectMapper();
    StringWriter writer = new StringWriter();

    try (JsonGenerator generator = mapper.getFactory().createGenerator(writer)) {
      for (JsonNode node : nodes) {
        mapper.writeValue(generator, node);
      }
    } catch (Exception e) {
      String message = JacksonUtils.formatException(e);
      throw new CliException(msg.failedToSerializeJsonSequence(message), e);
    }

    return writer.toString();
  }

  public static void serialize(Stream<JsonNode> nodes, OutputStream outputStream) {
    ObjectMapper mapper = new ObjectMapper();

    try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
      nodes.forEach(node -> {
        try {
          mapper.writeValue(generator, node);
        } catch (Exception e) {
          String message = JacksonUtils.formatException(e);
          throw new CliException(msg.failedToSerializeJsonSequence(message), e);
        }
      });
    } catch (CliException e) {
      throw e;
    } catch (Exception e) {
      String message = JacksonUtils.formatException(e);
      throw new CliException(msg.failedToSerializeJsonSequence(message), e);
    }
  }

  private static Spliterator<JsonNode> getSpliterator(Iterator<JsonNode> iterator) {
    Iterator<JsonNode> wrappedIterator = new Iterator<>() {
      @Override
      public boolean hasNext() {
        try {
          return iterator.hasNext();
        } catch (Exception e) {
          String message = JacksonUtils.formatException(e);
          throw new CliException(msg.failedToParseJson(message), e);
        }
      }

      @Override
      public JsonNode next() {
        try {
          return iterator.next();
        } catch (Exception e) {
          String message = JacksonUtils.formatException(e);
          throw new CliException(msg.failedToParseJson(message), e);
        }
      }
    };

    return Spliterators.spliteratorUnknownSize(wrappedIterator, Spliterator.ORDERED);
  }
}