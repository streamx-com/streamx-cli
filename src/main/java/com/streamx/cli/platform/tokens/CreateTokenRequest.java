package com.streamx.cli.platform.tokens;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record CreateTokenRequest(String name) {
}
