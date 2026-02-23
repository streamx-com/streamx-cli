package com.streamx.cli.ingestion;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.framework.CliException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Stream;

public class ConcatenatedJsonSerializer {

  public String serialize(List<JsonNode> nodes) {
    ObjectMapper mapper = new ObjectMapper();
    StringWriter writer = new StringWriter();

    try (JsonGenerator generator = mapper.getFactory().createGenerator(writer)) {
      for (JsonNode node : nodes) {
        mapper.writeValue(generator, node);
      }
    } catch (Exception e) {
      throw new CliException(msg.failedToSerializeJsonSequence(e.getMessage()), e);
    }

    return writer.toString();
  }

  public void serialize(Stream<JsonNode> nodes, OutputStream outputStream) {
    ObjectMapper mapper = new ObjectMapper();

    try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
      nodes.forEach(node -> {
        try {
          mapper.writeValue(generator, node);
        } catch (Exception e) {
          throw new CliException(msg.failedToSerializeJsonSequence(e.getMessage()), e);
        }
      });
    } catch (CliException e) {
      throw e;
    } catch (Exception e) {
      throw new CliException(msg.failedToSerializeJsonSequence(e.getMessage()), e);
    }
  }
}