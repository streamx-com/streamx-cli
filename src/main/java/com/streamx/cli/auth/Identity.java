package com.streamx.cli.auth;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Identity(
    String username,
    String name,
    String email,
    String subject,
    String issuer,
    String expiresAt,
    boolean expired
) {
}
