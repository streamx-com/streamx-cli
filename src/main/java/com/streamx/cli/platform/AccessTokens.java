package com.streamx.cli.platform;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.CliException;

/** The bearer credential for platform calls: a personal access token from the environment. */
public final class AccessTokens {

  public static final String STREAMX_PLATFORM_TOKEN = "STREAMX_PLATFORM_TOKEN";

  private AccessTokens() {
  }

  public static String current() {
    String token = platformTokenOverride();
    if (token == null) {
      throw new CliException(msg.platformTokenNotConfigured(STREAMX_PLATFORM_TOKEN));
    }
    return token;
  }

  private static String platformTokenOverride() {
    String value = System.getenv(STREAMX_PLATFORM_TOKEN);
    if (value == null || value.isBlank()) {
      value = System.getProperty(STREAMX_PLATFORM_TOKEN);
    }
    return value == null || value.isBlank() ? null : value.trim();
  }
}
