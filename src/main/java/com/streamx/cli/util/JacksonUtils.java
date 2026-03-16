package com.streamx.cli.util;

import com.fasterxml.jackson.core.JsonLocation;

public class JacksonUtils {
  public static String formatException(Exception e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t instanceof com.fasterxml.jackson.core.JsonProcessingException jpe) {
        String message = jpe.getOriginalMessage()
            .replaceAll(
                "\\s*\\(not recognized as one since Feature '.*?' not enabled for parser\\)",
                ""
            );
        JsonLocation loc = jpe.getLocation();
        if (loc != null) {
          return "%s (line: %d, column: %d)".formatted(
              message, loc.getLineNr(), loc.getColumnNr());
        }
        return message;
      }
    }
    return e.getMessage();
  }
}
