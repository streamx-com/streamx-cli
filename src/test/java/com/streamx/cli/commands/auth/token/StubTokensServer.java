package com.streamx.cli.commands.auth.token;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Stub of the platform profile/token endpoints used by the personal access token commands. */
public class StubTokensServer implements AutoCloseable {

  public static final String TOKEN = "sxp_v1_"
      + "0123456789abcdef0123456789abcdef" + "_"
      + "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789AbCdEfGh" + "_"
      + "Zz09Yx";
  public static final String TOKEN_ID = "0123456789abcdef0123456789abcdef";

  private final HttpServer server;
  private final List<String> requests = new ArrayList<>();
  private final List<String> authorizationHeaders = new ArrayList<>();
  private final List<String> requestBodies = new ArrayList<>();

  private volatile boolean empty;
  private volatile int forcedStatus;
  private volatile String forcedBody = "";

  public StubTokensServer() throws IOException {
    this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/api/v1/profile", this::route);
    server.start();
  }

  public String getUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  public List<String> getRequests() {
    return requests;
  }

  public List<String> getAuthorizationHeaders() {
    return authorizationHeaders;
  }

  public List<String> getRequestBodies() {
    return requestBodies;
  }

  public void returnNoTokens() {
    this.empty = true;
  }

  public void failWith(int status, String body) {
    this.forcedStatus = status;
    this.forcedBody = body;
  }

  private void route(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    String method = exchange.getRequestMethod();
    requests.add(method + " " + path);
    authorizationHeaders.add(
        String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
    requestBodies.add(new String(readBody(exchange), StandardCharsets.UTF_8));

    if (forcedStatus != 0) {
      respond(exchange, forcedStatus, forcedBody);
      return;
    }
    if (path.endsWith("/tokens") && "POST".equals(method)) {
      respond(exchange, 201, """
          {"id":"%s","name":"ci","token":"%s","createdAt":"2026-07-25T10:00:00Z"}
          """.formatted(TOKEN_ID, TOKEN));
      return;
    }
    if (path.endsWith("/tokens") && "GET".equals(method)) {
      respond(exchange, 200, empty ? "[]" : """
          [{"id":"%s","name":"ci","createdAt":"2026-07-25T10:00:00Z","lastUsedAt":null}]
          """.formatted(TOKEN_ID));
      return;
    }
    if ("DELETE".equals(method)) {
      respond(exchange, 204, "");
      return;
    }
    // GET /api/v1/profile - identity behind the credential, used by `auth whoami`.
    respond(exchange, 200, """
        {"userId":"user-1","email":"ci@streamx.com","firstName":"Ci","lastName":"Bot",
         "displayName":"Ci Bot"}
        """);
  }

  private static byte[] readBody(HttpExchange exchange) throws IOException {
    try (InputStream in = exchange.getRequestBody()) {
      return in.readAllBytes();
    }
  }

  private static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    if (bytes.length == 0) {
      exchange.sendResponseHeaders(status, -1);
    } else {
      exchange.sendResponseHeaders(status, bytes.length);
      exchange.getResponseBody().write(bytes);
    }
    exchange.close();
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
