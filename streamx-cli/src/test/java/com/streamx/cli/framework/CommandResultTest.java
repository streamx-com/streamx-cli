package com.streamx.cli.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.streamx.cli.framework.testing.TestObject;
import com.streamx.cli.framework.testing.UnserializableObject;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class CommandResultTest {
  private final TestObject result = new TestObject(
      null,
      true,
      100500,
      42.42,
      "Test string",
      new TestObject(
          null,
          false,
          7,
          3.14,
          "Nested object test string",
          null,
          null
      ),
      List.of(
          new TestObject(
              null,
              true,
              15,
              100.42,
              "Nested list object 1 test string",
              null,
              null
          ),
          new TestObject(
              null,
              false,
              18,
              0.42,
              "Nested list object 2 test string",
              null,
              null
          )
      )
  );
  private final CommandResult<TestObject> commandResult = new CommandResult<>(result);

  @Test
  void shouldUseTextFormatterWhenTextFormatProvided() throws CliException {
    String expectedOutput = """
        Void Value: null
        Boolean Value: true
        Long Value: 100500
        Float Value: 42.42
        String Value: Test string
        Nested Object Long Value: 7
        Nested Object String Value: Nested object test string
        Total Nested Objects: 2
        """;

    Function<CommandResult<TestObject>, String> textFormatter =
        cr -> """
        Void Value: %s
        Boolean Value: %b
        Long Value: %d
        Float Value: %.2f
        String Value: %s
        Nested Object Long Value: %d
        Nested Object String Value: %s
        Total Nested Objects: %d
        """.formatted(
            cr.result.voidValue,
            cr.result.booleanValue,
            cr.result.longValue,
            cr.result.floatValue,
            cr.result.stringValue,
            cr.result.nestedObject.longValue,
            cr.result.nestedObject.stringValue,
            cr.result.nestedObjects.size()
        );

    String output = commandResult.toText(OutputFormat.text, textFormatter);

    assertEquals(expectedOutput, output);
  }

  @Test
  void shouldReturnPrettyPrintedJsonWhenJsonFormatProvided() {
    String expectedOutput = """
         {
          "voidValue" : null,
          "booleanValue" : true,
          "longValue" : 100500,
          "floatValue" : 42.42,
          "stringValue" : "Test string",
          "nestedObject" : {
            "voidValue" : null,
            "booleanValue" : false,
            "longValue" : 7,
            "floatValue" : 3.14,
            "stringValue" : "Nested object test string",
            "nestedObject" : null,
            "nestedObjects" : null
          },
          "nestedObjects" : [ {
            "voidValue" : null,
            "booleanValue" : true,
            "longValue" : 15,
            "floatValue" : 100.42,
            "stringValue" : "Nested list object 1 test string",
            "nestedObject" : null,
            "nestedObjects" : null
          }, {
            "voidValue" : null,
            "booleanValue" : false,
            "longValue" : 18,
            "floatValue" : 0.42,
            "stringValue" : "Nested list object 2 test string",
            "nestedObject" : null,
            "nestedObjects" : null
          } ]
        }
        """.strip();

    String output = commandResult.toText(OutputFormat.json, null);

    assertEquals(expectedOutput, output);
  }

  @Test
  void shouldReturnYamlWhenYamlFormatProvided() {
    String expectedOutput = """
        voidValue: null
        booleanValue: true
        longValue: 100500
        floatValue: 42.42
        stringValue: "Test string"
        nestedObject:
          voidValue: null
          booleanValue: false
          longValue: 7
          floatValue: 3.14
          stringValue: "Nested object test string"
          nestedObject: null
          nestedObjects: null
        nestedObjects:
        - voidValue: null
          booleanValue: true
          longValue: 15
          floatValue: 100.42
          stringValue: "Nested list object 1 test string"
          nestedObject: null
          nestedObjects: null
        - voidValue: null
          booleanValue: false
          longValue: 18
          floatValue: 0.42
          stringValue: "Nested list object 2 test string"
          nestedObject: null
          nestedObjects: null
        """.strip();

    String output = commandResult.toText(OutputFormat.yaml, null);

    assertEquals(expectedOutput, output);
  }

  @Test
  void shouldHandleNullResultGracefully() {
    CommandResult<TestObject> commandResult = new CommandResult<>(null);

    String jsonOutput = commandResult.toText(OutputFormat.json, null);
    String yamlOutput  = commandResult.toText(OutputFormat.yaml, null);

    assertEquals("null", jsonOutput);
    assertEquals("null", yamlOutput);
  }

  @Test
  void shouldThrowCliExceptionForUnserializableObject() {
    UnserializableObject unserializable = new UnserializableObject();
    CommandResult<UnserializableObject> commandResult = new CommandResult<>(unserializable);

    assertThrows(CliException.class, () ->
        commandResult.toText(OutputFormat.json, null)
    );
  }
}