package com.streamx.cli.auth;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.framework.CliException;
import java.io.IOException;
import java.util.Base64;

public final class AccessTokenClaims {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private AccessTokenClaims() {
  }

  public static JsonNode of(String accessToken) {
    String[] parts = accessToken.split("\\.");
    if (parts.length < 2) {
      throw new CliException(msg.authTokenMalformed());
    }
    try {
      return MAPPER.readTree(Base64.getUrlDecoder().decode(parts[1]));
    } catch (IOException | IllegalArgumentException e) {
      throw new CliException(msg.authTokenMalformed(), e);
    }
  }
}
