package com.streamx.cli.platform.tokens;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record TokenSummary(String id, String name, String createdAt, String lastUsedAt) {
}
