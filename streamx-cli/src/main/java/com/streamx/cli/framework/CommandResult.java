package com.streamx.cli.framework;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.util.function.Function;

/**
 * @param <ResultT> Must be serializable by Jackson (POJO, JsonSerializable, etc.)
 */
public class CommandResult<ResultT> {
  public ResultT result;

  public CommandResult(ResultT result) {
    this.result = result;
  }

  public String toText(
      OutputFormat outputFormat,
      Function<CommandResult<ResultT>, String> textFormatter
  ) throws CliException {
    try {
      switch (outputFormat) {
        case OutputFormat.text -> {
          return textFormatter.apply(this);
        }
        case OutputFormat.json -> {
          ObjectMapper mapper = new ObjectMapper();
          JsonNode jsonNode = mapper.valueToTree(result);
          return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        }
        case OutputFormat.yaml -> {
          YAMLFactory yamlFactory = YAMLFactory.builder()
              .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
              .build();
          ObjectMapper mapper = new ObjectMapper(yamlFactory);
          JsonNode jsonNode = mapper.valueToTree(result);
          return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode).strip();
        }
        default -> throw new CliException(msg.unsupportedOutputFormat());
      }
    } catch (JsonProcessingException e) {
      throw new CliException(msg.unableToSerializeJson(), e);
    }
  }
}
