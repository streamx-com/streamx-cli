package com.streamx.cli.ingestion;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.util.JacksonUtils;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ConcatenatedJsonParser {
  public List<JsonNode> parse(String input) {
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

  public Stream<JsonNode> parse(InputStream inputStream) {
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