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
  private volatile boolean repositoryConnected = true;
  private volatile boolean sshKeyExists;

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

  public void returnNoRepository() {
    this.repositoryConnected = false;
  }

  public void sshKeyExists(boolean exists) {
    this.sshKeyExists = exists;
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

    if (path.contains("/repository")) {
      handleRepository(exchange, method, path);
    } else if (path.endsWith("/status")) {
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
    } else if (path.endsWith("/clusters")) {
      handleClusters(exchange, method);
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

  private void handleRepository(HttpExchange exchange, String method, String path)
      throws IOException {
    if (path.endsWith("/repository/ssh-key/generate-key-pair")) {
      respond(exchange, 200,
          "{\"privateKey\":\"GENERATED-PRIVATE\",\"publicKey\":\"ssh-ed25519 GENERATED-PUBLIC\"}");
    } else if (path.endsWith("/repository/ssh-key/exists")) {
      respond(exchange, 200, String.valueOf(sshKeyExists));
    } else if (path.endsWith("/repository/ssh-key/public-key")) {
      if (sshKeyExists) {
        respond(exchange, 200, "{\"publicKey\":\"ssh-ed25519 STORED-PUBLIC\"}");
      } else {
        respond(exchange, 404, "{\"errorCode\":\"NOT_FOUND\",\"message\":\"no key\"}");
      }
    } else if (path.endsWith("/repository/ssh-key")) {
      switch (method) {
        case "POST", "PATCH" -> {
          sshKeyExists = true;
          respond(exchange, 200, "{}");
        }
        case "DELETE" -> {
          if (sshKeyExists) {
            sshKeyExists = false;
            respond(exchange, 204, "");
          } else {
            respond(exchange, 404, "{\"errorCode\":\"NOT_FOUND\",\"message\":\"no key\"}");
          }
        }
        default -> respond(exchange, 405, "");
      }
    } else {
      switch (method) {
        case "GET" -> {
          if (repositoryConnected) {
            respond(exchange, 200, """
                {"name":"web","uri":"git@github.com:acme/web.git","branch":"main",
                 "commitId":"abc1234","lastSynced":1784800000,
                 "projectRepositoryStatus":{"ready":true,"errorMessages":[]},
                 "sshKeyProvided":%s}
                """.formatted(sshKeyExists));
          } else {
            respond(exchange, 404, "{\"errorCode\":\"NOT_FOUND\",\"message\":\"no repository\"}");
          }
        }
        case "POST" -> {
          repositoryConnected = true;
          respond(exchange, 201, "");
        }
        case "PATCH" -> {
          if (repositoryConnected) {
            respond(exchange, 200, "{}");
          } else {
            respond(exchange, 404, "{\"errorCode\":\"NOT_FOUND\",\"message\":\"no repository\"}");
          }
        }
        case "DELETE" -> {
          if (repositoryConnected) {
            repositoryConnected = false;
            respond(exchange, 204, "");
          } else {
            respond(exchange, 404, "{\"errorCode\":\"NOT_FOUND\",\"message\":\"no repository\"}");
          }
        }
        default -> respond(exchange, 405, "");
      }
    }
  }

  private void handleClusters(HttpExchange exchange, String method) throws IOException {
    if ("PATCH".equals(method)) {
      respond(exchange, 200, "{}");
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
