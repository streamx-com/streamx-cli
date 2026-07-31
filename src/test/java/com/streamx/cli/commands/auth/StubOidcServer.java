package com.streamx.cli.commands.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class StubOidcServer implements AutoCloseable {
  public static final String USER_CODE = "WDJB-MJHT";
  public static final String DEVICE_CODE = "test-device-code";
  public static final String REFRESH_TOKEN = "test-refresh-token";

  public static final String USERNAME = "user1";
  public static final String EMAIL = "user1@streamx.com";
  public static final String SUBJECT = "0a084fdd-7b0c-4ee0-821d-37c78fb43c09";
  public static final String AUTH_CODE = "test-auth-code";

  public static final String ACCESS_TOKEN = unsignedJwt("""
      {"preferred_username":"%s","name":"User First","email":"%s","sub":"%s",
       "iss":"https://keycloak.example/realms/streamx","azp":"streamx-cli"}
      """.formatted(USERNAME, EMAIL, SUBJECT));

  private static String unsignedJwt(String claims) {
    Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    return encoder.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8))
        + "." + encoder.encodeToString(claims.getBytes(StandardCharsets.UTF_8))
        + ".signature";
  }

  private final HttpServer server;
  private final String realm;

  private final int pendingResponses;

  private final AtomicInteger tokenRequests = new AtomicInteger();
  private final List<String> revokedTokens = new ArrayList<>();

  private volatile String tokenError;
  private volatile int tokenStatus;
  private volatile String tokenBody = "";
  private volatile boolean authorizationDenied;
  private volatile boolean wrongIssuer;
  private volatile boolean offlineRequested;
  private volatile boolean offlineScopeRefused;
  private volatile boolean revocationFails;
  private volatile String lastRequestedScope = "";
  private volatile String lastTokenRequestBody = "";
  private final Map<String, String> lastAuthorizationRequest = new java.util.HashMap<>();

  public StubOidcServer(String realm, int pendingResponses) throws IOException {
    this.realm = realm;
    this.pendingResponses = pendingResponses;
    this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

    String base = "/realms/" + realm;
    server.createContext(base + "/.well-known/openid-configuration", this::handleDiscovery);
    server.createContext(base + "/auth", this::handleAuthorization);
    server.createContext(base + "/device", this::handleDeviceAuthorization);
    server.createContext(base + "/token", this::handleToken);
    server.createContext(base + "/revoke", this::handleRevoke);
    server.start();
  }

  public String getServerUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  public int getTokenRequestCount() {
    return tokenRequests.get();
  }

  public List<String> getRevokedTokens() {
    return revokedTokens;
  }

  public void failTokenWith(String error) {
    this.tokenError = error;
  }

  public void failTokenWithStatus(int status, String body) {
    this.tokenStatus = status;
    this.tokenBody = body;
  }

  public void denyAuthorization() {
    this.authorizationDenied = true;
  }

  public void returnWrongIssuer() {
    this.wrongIssuer = true;
  }

  public void refuseOfflineScope() {
    this.offlineScopeRefused = true;
  }

  public void failRevocation() {
    this.revocationFails = true;
  }

  public Map<String, String> getLastAuthorizationRequest() {
    return lastAuthorizationRequest;
  }

  public String getLastTokenRequestBody() {
    return lastTokenRequestBody;
  }

  public String getLastRequestedScope() {
    return lastRequestedScope;
  }

  private static Map<String, String> parseQuery(String rawQuery) {
    Map<String, String> params = new java.util.HashMap<>();
    if (rawQuery == null) {
      return params;
    }
    for (String pair : rawQuery.split("&")) {
      int eq = pair.indexOf('=');
      if (eq > 0) {
        params.put(
            java.net.URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
            java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
      }
    }
    return params;
  }

  private void handleDiscovery(HttpExchange exchange) throws IOException {
    String base = getServerUrl() + "/realms/" + realm;
    String issuer = wrongIssuer ? "https://evil.example/realms/" + realm : base;
    respond(exchange, 200, """
        {
          "issuer": "%s",
          "authorization_endpoint": "%s/auth",
          "device_authorization_endpoint": "%s/device",
          "token_endpoint": "%s/token",
          "revocation_endpoint": "%s/revoke"
        }
        """.formatted(issuer, base, base, base, base));
  }

  private void handleAuthorization(HttpExchange exchange) throws IOException {
    Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
    lastAuthorizationRequest.putAll(query);
    recordScope(exchange.getRequestURI().getRawQuery());

    String redirectUri = query.get("redirect_uri");
    String state = query.get("state");
    String location = authorizationDenied
        ? redirectUri + "?error=access_denied&state=" + state
        : redirectUri + "?code=" + AUTH_CODE + "&state=" + state;

    exchange.getResponseHeaders().add("Location", location);
    exchange.sendResponseHeaders(302, -1);
    exchange.close();
  }

  private void handleDeviceAuthorization(HttpExchange exchange) throws IOException {
    recordScope(readBody(exchange));
    respond(exchange, 200, """
        {
          "device_code": "%s",
          "user_code": "%s",
          "verification_uri": "%s/device",
          "verification_uri_complete": "%s/device?user_code=%s",
          "interval": 1,
          "expires_in": 60
        }
        """.formatted(DEVICE_CODE, USER_CODE, getServerUrl(), getServerUrl(), USER_CODE));
  }

  private void handleToken(HttpExchange exchange) throws IOException {
    lastTokenRequestBody = readBody(exchange);
    int attempt = tokenRequests.incrementAndGet();

    if (tokenStatus != 0) {
      respond(exchange, tokenStatus, tokenBody);
      return;
    }
    if (tokenError != null) {
      respond(exchange, 400, "{\"error\": \"%s\"}".formatted(tokenError));
      return;
    }
    if (attempt <= pendingResponses) {
      respond(exchange, 400, "{\"error\": \"authorization_pending\"}");
      return;
    }

    String refreshToken = offlineRequested ? getOfflineToken() : REFRESH_TOKEN;
    respond(exchange, 200, """
        {
          "access_token": "%s",
          "refresh_token": "%s",
          "expires_in": 300,
          "token_type": "Bearer"
        }
        """.formatted(ACCESS_TOKEN, refreshToken));
  }

  public String getOfflineToken() {
    return unsignedJwt("""
        {"typ":"Offline","iss":"%s/realms/%s","azp":"streamx-cli","sub":"%s"}
        """.formatted(getServerUrl(), realm, SUBJECT));
  }

  private void recordScope(String formOrQuery) {
    String scope = parseQuery(formOrQuery).getOrDefault("scope", "");
    lastRequestedScope = scope;
    if (scope.contains("offline_access") && !offlineScopeRefused) {
      offlineRequested = true;
    }
  }

  private void handleRevoke(HttpExchange exchange) throws IOException {
    String body = readBody(exchange);
    if (revocationFails) {
      respond(exchange, 503, "{\"error\":\"temporarily_unavailable\"}");
      return;
    }
    for (String param : body.split("&")) {
      if (param.startsWith("token=")) {
        revokedTokens.add(
            java.net.URLDecoder.decode(param.substring("token=".length()), StandardCharsets.UTF_8));
      }
    }
    respond(exchange, 200, "{}");
  }

  private static String readBody(HttpExchange exchange) throws IOException {
    try (InputStream in = exchange.getRequestBody()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
