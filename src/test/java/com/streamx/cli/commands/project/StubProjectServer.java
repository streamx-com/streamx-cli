package com.streamx.cli.commands.project;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Stand-in for the Projects Resource, so project commands can be tested without a deployment. */
public class StubProjectServer implements AutoCloseable {

  private final HttpServer server;
  private final List<String> requests = new ArrayList<>();
  private final List<String> rawRequests = new ArrayList<>();
  private final List<String> requestBodies = new ArrayList<>();

  private volatile int forcedStatus;
  private volatile String forcedBody = "";
  private volatile boolean empty;

  public StubProjectServer() throws IOException {
    this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/api/v1/organizations", this::route);
    server.start();
  }

  public String getUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  public List<String> getRequests() {
    return requests;
  }

  public List<String> getRawRequests() {
    return rawRequests;
  }

  public List<String> getRequestBodies() {
    return requestBodies;
  }

  public void failWith(int status, String body) {
    this.forcedStatus = status;
    this.forcedBody = body;
  }

  public void returnNoProjects() {
    this.empty = true;
  }

  private void route(HttpExchange exchange) throws IOException {
    String method = exchange.getRequestMethod();
    String path = exchange.getRequestURI().getPath();
    String body = new String(readBody(exchange), StandardCharsets.UTF_8);
    requests.add(method + " " + path);
    rawRequests.add(method + " " + exchange.getRequestURI().getRawPath());
    requestBodies.add(body);

    if (forcedStatus != 0) {
      respond(exchange, forcedStatus, forcedBody);
      return;
    }

    if (path.endsWith("/status")) {
      respond(exchange, 200, """
          {"state":"Pending","statuses":[
            {"state":"Ready","reason":"Deployed","message":"web is running"},
            {"state":"Pending","reason":"Scaling","message":"api starting"}
          ]}
          """);
    } else if (path.endsWith("/changes/pending")) {
      respond(exchange, 200, """
          [{"message":"web will be created","details":["image: nginx","replicas: 2"]}]
          """);
    } else if (path.endsWith("/projects")) {
      handleCollection(exchange, method, body);
    } else {
      handleItem(exchange, method, body);
    }
  }

  private void handleCollection(HttpExchange exchange, String method, String body)
      throws IOException {
    if ("POST".equals(method)) {
      String name = extract(body, "name");
      // The id is server-derived; the CLI must read it from the response, so it differs from name.
      respond(exchange, 201,
          project("so-" + name.replace("-", ""), name, extract(body, "description"), "Pending"));
    } else if (empty) {
      respond(exchange, 200, "[]");
    } else {
      respond(exchange, 200, """
          [
            %s,
            %s
          ]
          """.formatted(
          project("so-org-web-a1b2c", "web", "Frontend", "Ready"),
          project("so-org-api-d3e4f", "api", "Backend", "Pending")));
    }
  }

  private void handleItem(HttpExchange exchange, String method, String body) throws IOException {
    String id = exchange.getRequestURI().getPath()
        .replaceAll(".*/projects/", "").replaceAll("/.*", "");
    switch (method) {
      case "GET" -> respond(exchange, 200, project(id, "web", "Frontend", "Ready"));
      case "PATCH" -> respond(exchange, 200,
          project(id, extract(body, "name"), extract(body, "description"), "Ready"));
      case "DELETE" -> respond(exchange, 204, "");
      default -> respond(exchange, 405, "");
    }
  }

  private static String project(String id, String name, String description, String state) {
    return "{\"id\":\"%s\",\"name\":\"%s\",\"description\":\"%s\",\"state\":\"%s\"}"
        .formatted(id, name, description, state);
  }

  private static String extract(String body, String field) {
    java.util.regex.Matcher matcher = java.util.regex.Pattern
        .compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"")
        .matcher(body);
    return matcher.find() ? matcher.group(1) : "";
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
