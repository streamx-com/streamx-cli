package com.streamx.cli.commands.profile.list;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ProfileInfo(String name, boolean active, String platformUrl, boolean loggedIn) {
}
