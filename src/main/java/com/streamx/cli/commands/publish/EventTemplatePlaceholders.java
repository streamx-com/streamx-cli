package com.streamx.cli.commands.publish;

import static com.streamx.cli.i18n.MessageProvider.msg;

import java.util.List;

/**
 * This class defines supported placeholders in event templates.
 * Each placeholder corresponds to a dynamic value that can be replaced in
 * the event template during processing.
 */
public class EventTemplatePlaceholders {

  public static final String PAYLOAD_PATH = "${payloadPath}";
  public static final String PAYLOAD_CONTENT_BASE64 = "file://${payloadPath}";
  public static final String PAYLOAD_CONTENT_JSON = "json://${payloadPath}";
  public static final String RELATIVE_PATH = "${relativePath}";
  public static final String SUBJECT = "${subject}";
  public static final String UUID = "${uuid}";
  public static final String CURRENT_TIME = "${currentTime}";

  public record Placeholder(String name, String description) {
  }

  public static List<Placeholder> all() {
    return List.of(
        new Placeholder(PAYLOAD_PATH, msg.placeholderDescriptionPayloadPath()),
        new Placeholder(PAYLOAD_CONTENT_BASE64, msg.placeholderDescriptionPayloadContentBase64()),
        new Placeholder(PAYLOAD_CONTENT_JSON, msg.placeholderDescriptionPayloadContentJson()),
        new Placeholder(RELATIVE_PATH, msg.placeholderDescriptionRelativePath()),
        new Placeholder(SUBJECT, msg.placeholderDescriptionSubject()),
        new Placeholder(UUID, msg.placeholderDescriptionUuid()),
        new Placeholder(CURRENT_TIME, msg.placeholderDescriptionCurrentTime())
    );
  }
}
