package com.streamx.cli.auth;

import java.time.Instant;

public record Credentials(
    String accessToken,
    String refreshToken,
    Instant expiresAt,
    String issuerUrl,
    String clientId,
    boolean insecure
) {
}
