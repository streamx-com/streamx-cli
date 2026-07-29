package com.streamx.cli.auth;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.CliException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public record Pkce(String verifier, String challenge) {

  public static final String METHOD = "S256";

  public static Pkce generate() {
    String verifier = randomUrlSafe(64);
    return new Pkce(verifier, challenge(verifier));
  }

  public static String randomUrlSafe(int bytes) {
    byte[] buffer = new byte[bytes];
    // Per-call, never a static field: GraalVM rejects a SecureRandom in the native image heap.
    new SecureRandom().nextBytes(buffer);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
  }

  private static String challenge(String verifier) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(verifier.getBytes(StandardCharsets.US_ASCII));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new CliException(msg.authPkceFailed(e.getMessage()), e);
    }
  }
}
