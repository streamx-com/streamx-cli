package com.streamx.cli.commands.auth;

import com.streamx.cli.auth.AuthConfig;

public final class AuthTestEndpoints {
  public static final String SERVER_URL_PROPERTY = "streamx.test.auth.server-url";
  public static final String REALM_PROPERTY = "streamx.test.auth.realm";
  public static final String CLIENT_ID_PROPERTY = "streamx.test.auth.client-id";
  public static final String USERNAME_PROPERTY = "streamx.test.auth.username";
  public static final String PASSWORD_PROPERTY = "streamx.test.auth.password";
  public static final String INSECURE_PROPERTY = "streamx.test.auth.insecure";

  private AuthTestEndpoints() {
  }

  public static String serverUrl() {
    return System.getProperty(SERVER_URL_PROPERTY);
  }

  public static String realm() {
    return System.getProperty(REALM_PROPERTY, AuthConfig.DEFAULT_REALM);
  }

  public static String clientId() {
    return System.getProperty(CLIENT_ID_PROPERTY, AuthConfig.DEFAULT_CLIENT_ID);
  }

  public static String username() {
    return System.getProperty(USERNAME_PROPERTY, "user1");
  }

  public static String password() {
    return System.getProperty(PASSWORD_PROPERTY, "user1");
  }

  public static boolean insecure() {
    return Boolean.parseBoolean(System.getProperty(INSECURE_PROPERTY, "true"));
  }
}
