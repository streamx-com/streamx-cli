package com.streamx.cli.commands.settings.eventtemplates.validate;

import java.util.List;

public record ValidateCommandResult(
    List<TemplateValidation> results,
    int validCount,
    int invalidCount
) {
  public record TemplateValidation(
      String id,
      String path,
      boolean valid,
      String error
  ) {
  }
}
