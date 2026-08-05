package com.streamx.cli.commands.context.list;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ContextInfo(String name, boolean active, String platformUrl, boolean loggedIn) {
}
