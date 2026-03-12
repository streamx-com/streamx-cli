package com.streamx.cli.framework;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.util.Optional;
import java.util.function.Function;

/**
 * @param <ResultT> Must be serializable by Jackson (POJO, JsonSerializable, etc.)
 */
public class CommandResult<ResultT> {
  private final ResultT data;

  /** Optional error attached to the result.
   * It may be useful for commands which can partially fail.
  */
  private CliException error;

  private Integer exitCodeOverride;

  public CommandResult(ResultT data) {
    this.data = data;
  }

  public ResultT getData() {
    return data;
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
          JsonNode jsonNode = mapper.valueToTree(data);
          return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        }
        case OutputFormat.yaml -> {
          YAMLFactory yamlFactory = YAMLFactory.builder()
              .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
              .build();
          ObjectMapper mapper = new ObjectMapper(yamlFactory);
          JsonNode jsonNode = mapper.valueToTree(data);
          String formattedJsonNode = mapper
              .writerWithDefaultPrettyPrinter()
              .writeValueAsString(jsonNode)
              .strip();

          if (
              (formattedJsonNode.isEmpty() && jsonNode.isTextual())
                  || formattedJsonNode.equals("--- \"\"")
          ) {
            return "\"\"";
          }

          return formattedJsonNode;
        }
        default -> throw new CliException(msg.unsupportedOutputFormat());
      }
    } catch (Exception e) {
      throw new CliException(msg.unableToSerializeJson(), e);
    }
  }

  public Optional<CliException> getError() {
    return Optional.ofNullable(error);
  }

  public void setError(CliException error) {
    this.error = error;
  }

  public Optional<Integer> getExitCodeOverride() {
    return Optional.ofNullable(exitCodeOverride);
  }

  public void setExitCodeOverride(int exitCodeOverride) {
    this.exitCodeOverride = exitCodeOverride;
  }
}
