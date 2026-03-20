package com.streamx.cli.commands.publish.events;

import com.fasterxml.jackson.databind.JsonNode;

record TemplateContext(
    JsonNode template,
    String templatePath,
    String appliedPatch,
    String patchPath
) {
}