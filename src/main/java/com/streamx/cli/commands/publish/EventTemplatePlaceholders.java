package com.streamx.cli.commands.publish;

/**
 * This class defines supported placeholders in event templates.
 * Each placeholder corresponds to a dynamic value that can be replaced in
 * the event template during processing.
 */
public class EventTemplatePlaceholders {

  // Placeholder for the payload path
  public static final String PAYLOAD_PATH = "${payloadPath}";

  // Placeholder for the base64 encoded payload file content
  public static final String PAYLOAD_CONTENT_BASE64 = "file://${payloadPath}";

  // Placeholder for the relative path of the event payload file.
  // The syntax ${relativePath:n} is supported, where 'n' specifies the number of parent directories
  // to traverse from the event payload file's location. For example, ${relativePath:3} will resolve
  // to the path of the 3rd parent directory.
  public static final String RELATIVE_PATH = "${relativePath}";

  // Placeholder for the subject of the event
  public static final String SUBJECT = "${subject}";

  // Placeholder for a universally unique identifier (UUID v4)
  public static final String UUID = "${uuid}";

  // Placeholder for the current timestamp in ISO_OFFSET_DATE_TIME format
  public static final String CURRENT_TIME = "${currentTime}";
}