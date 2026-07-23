package com.streamx.cli.commands.org;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class StubPlatformServer implements AutoCloseable {
  private final HttpServer server;
  private final List<String> authorizationHeaders = new ArrayList<>();
  private final List<String> requests = new ArrayList<>();
  private final List<String> createdNames = new ArrayList<>();
  private final List<String> deletedIds = new ArrayList<>();

  private volatile int forcedStatus;
  private volatile String forcedBody = "";
  private volatile boolean empty;

  private final List<String> requestBodies = new ArrayList<>();

  /**
   * Raw, still-encoded request paths. {@code getRequestURI().getPath()} decodes, which makes an
   * escaped segment indistinguishable from one that injected extra path segments.
   */
  private final List<String> rawRequests = new ArrayList<>();

  public List<String> getRawRequests() {
    return rawRequests;
  }

  public StubPlatformServer() throws IOException {
    this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/api/v1/organizations", this::route);
    server.start();
  }

  public List<String> getRequestBodies() {
    return requestBodies;
  }

  /** Sub-resources are matched before the plain organization paths. */
  private void route(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    if (path.contains("/users")) {
      handleUsers(exchange);
    } else if (path.contains("/invitations")) {
      handleInvitations(exchange);
    } else if (path.endsWith("/clusters")) {
      handleClusters(exchange);
    } else if (path.endsWith("/projects")) {
      handleProjects(exchange);
    } else {
      handleOrganizations(exchange);
    }
  }

  private void handleUsers(HttpExchange exchange) throws IOException {
    String method = exchange.getRequestMethod();
    String path = exchange.getRequestURI().getPath();
    record(exchange, method, path);
    if (forcedStatus != 0) {
      respond(exchange, forcedStatus, forcedBody);
      return;
    }

    if ("GET".equals(method)) {
      respond(exchange, 200, """
          [
            {"id":"user1@streamx.com","displayName":"User First",
             "role":{"name":"owner","displayName":"Owner"},"status":"ACTIVE","isCaller":true},
            {"id":"active@streamx.com","displayName":"Active Member",
             "role":{"name":"edit","displayName":"Editor"},"status":"ACTIVE","isCaller":false},
            {"id":"pending@streamx.com","displayName":"Pending Invitee",
             "role":{"name":"view","displayName":"Viewer"},"status":"PENDING","isCaller":false}
          ]
          """);
    } else {
      respond(exchange, 204, "");
    }
  }

  private void handleInvitations(HttpExchange exchange) throws IOException {
    String method = exchange.getRequestMethod();
    String path = exchange.getRequestURI().getPath();
    record(exchange, method, path);
    if (forcedStatus != 0) {
      respond(exchange, forcedStatus, forcedBody);
      return;
    }

    if ("GET".equals(method)) {
      respond(exchange, 200, """
          [
            {"email":"invited@streamx.com","role":{"name":"edit","displayName":"Editor"},
             "status":"PENDING"}
          ]
          """);
    } else {
      respond(exchange, 204, "");
    }
  }

  private void handleProjects(HttpExchange exchange) throws IOException {
    record(exchange, exchange.getRequestMethod(), exchange.getRequestURI().getPath());
    if (forcedStatus != 0) {
      respond(exchange, forcedStatus, forcedBody);
      return;
    }
    respond(exchange, 200, """
        [
          {"id":"so-acme-shop-a1b2c","name":"shop","state":"Ready"},
          {"id":"so-acme-site-d3e4f","name":"site","state":"Ready"}
        ]
        """);
  }

  private void handleClusters(HttpExchange exchange) throws IOException {
    record(exchange, exchange.getRequestMethod(), exchange.getRequestURI().getPath());
    if (forcedStatus != 0) {
      respond(exchange, forcedStatus, forcedBody);
      return;
    }
    respond(exchange, 200, """
        {
          "processing": [
            {"id":"processing-eu-central","enabled":true,"name":"EU Central",
             "location":{"latitude":50.1,"longitude":8.6}}
          ],
          "edge": [
            {"id":"edge-us-east","enabled":false,"name":"US East",
             "location":{"latitude":40.7,"longitude":-74.0}}
          ]
        }
        """);
  }

  private void record(HttpExchange exchange, String method, String path) throws IOException {
    requests.add(method + " " + path);
    rawRequests.add(method + " " + exchange.getRequestURI().getRawPath());
    authorizationHeaders.add(
        String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
    requestBodies.add(new String(readBody(exchange), StandardCharsets.UTF_8));
  }

  public String getUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  public List<String> getAuthorizationHeaders() {
    return authorizationHeaders;
  }

  public List<String> getRequests() {
    return requests;
  }

  public List<String> getCreatedNames() {
    return createdNames;
  }

  public List<String> getDeletedIds() {
    return deletedIds;
  }

  public void failWith(int status, String body) {
    this.forcedStatus = status;
    this.forcedBody = body;
  }

  public void returnNoOrganizations() {
    this.empty = true;
  }

  private void handleOrganizations(HttpExchange exchange) throws IOException {
    String method = exchange.getRequestMethod();
    String path = exchange.getRequestURI().getPath();
    requests.add(method + " " + path);
    rawRequests.add(method + " " + exchange.getRequestURI().getRawPath());
    authorizationHeaders.add(
        String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));

    if (forcedStatus != 0) {
      respond(exchange, forcedStatus, forcedBody);
      return;
    }

    String id = path.substring("/api/v1/organizations".length()).replaceAll("^/", "");

    if ("GET".equals(method) && id.isEmpty() && empty) {
      respond(exchange, 200, "[]");
    } else if ("GET".equals(method) && id.isEmpty()) {
      respond(exchange, 200, """
          [
            {"id":"acme","name":"Acme","projectsNumber":"3",
             "role":{"name":"owner","displayName":"Owner"},"state":"ready"},
            {"id":"globex","name":"Globex","projectsNumber":"0",
             "role":{"name":"view","displayName":"Viewer"},"state":"ready"}
          ]
          """);
    } else if ("GET".equals(method)) {
      respond(exchange, 200, """
          {"id":"%s","name":"Acme","projectsNumber":"3",
           "role":{"name":"owner","displayName":"Owner"},"state":"ready"}
          """.formatted(id));
    } else if ("POST".equals(method)) {
      String body = new String(readBody(exchange), StandardCharsets.UTF_8);
      createdNames.add(body.replaceAll(".*\"name\"\\s*:\\s*\"([^\"]+)\".*", "$1"));
      respond(exchange, 204, "");
    } else if ("DELETE".equals(method)) {
      deletedIds.add(id);
      respond(exchange, 204, "");
    } else {
      respond(exchange, 405, "");
    }
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
