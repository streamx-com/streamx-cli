package com.streamx.cli.commands.publish.event;

import com.fasterxml.jackson.databind.JsonNode;

record EventCommandResult(
    String subject,
    String templatePath,
    JsonNode event
) {}
