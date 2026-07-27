package com.streamx.cli.platform.tokens;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record TokenResponse(String id, String name, String token, String createdAt) {
}
