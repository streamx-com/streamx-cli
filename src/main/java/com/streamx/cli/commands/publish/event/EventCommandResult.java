package com.streamx.cli.commands.publish.event;

import com.fasterxml.jackson.databind.JsonNode;

public record EventCommandResult(
    String error,
    String subject,
    String templatePath,
    String template,
    JsonNode event
) {}
