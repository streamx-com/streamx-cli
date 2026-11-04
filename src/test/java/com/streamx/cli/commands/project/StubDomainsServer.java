package com.streamx.cli.commands.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stand-in for the Domains resource of one project. A PUT answers with the new list at once but
 * applies it only after {@link #applyAfterReads} GETs, like a project with a repository.
 */
public class StubDomainsServer implements AutoCloseable {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final HttpServer server;
  private final List<String> requests = new ArrayList<>();
  private final List<String> requestBodies = new ArrayList<>();
  private final Deque<String> queuedLists = new ArrayDeque<>();
  private final Deque<String> queuedChecks = new ArrayDeque<>();
  private final Map<String, String> applied = new LinkedHashMap<>();

  private Map<String, String> saved;
  private int readsUntilApplied;
  private int applyAfterReads;
  private int forcedStatus;
  private int forcedPutStatus;
  private String forcedBody = "";

  public StubDomainsServer() throws IOException {
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

  public List<String> getRequestBodies() {
    return requestBodies;
  }

  /** Domains the project has applied, as built by {@link #domain}. */
  public synchronized void applied(String... domains) {
    applied.clear();
    for (String domain : domains) {
      applied.put(field(domain, "domain"), domain);
    }
  }

  /** GET answers to give, in order, before answering with the applied domains again. */
  public synchronized void queueLists(String... lists) {
    queuedLists.addAll(List.of(lists));
  }

  /** DNS check answers to give, in order; the last one repeats. */
  public synchronized void queueChecks(String... checks) {
    queuedChecks.addAll(List.of(checks));
  }

  /** Saved lists apply after this many GETs; a negative number means never. */
  public synchronized void applyAfterReads(int reads) {
    this.applyAfterReads = reads;
  }

  public synchronized void failWith(int status, String body) {
    this.forcedStatus = status;
    this.forcedBody = body;
  }

  public synchronized void failPutsWith(int status, String body) {
    this.forcedPutStatus = status;
    this.forcedBody = body;
  }

  /** A domain whose ID is the hostname's first label, as the platform derives it. */
  public static String domain(String host, String state, String reason, String lastCheckedAt) {
    return """
        {"id":"%s","domain":"%s","state":"%s",%s"attempts":2,%s"nextCheckAt":"2026-10-29T08:05:00Z",
         "dnsRecords":[
           {"type":"TXT","name":"_streamx-verification.%s",
            "value":"streamx-domain-verification=b3Jn"},
           {"type":"CNAME","name":"_acme-challenge.%s","value":"0a1b2c3d.acme.example.net"}]}
        """.formatted(host.substring(0, host.indexOf('.')), host, state,
        reason == null ? "" : "\"failureReason\":\"" + reason + "\",",
        lastCheckedAt == null ? "" : "\"lastCheckedAt\":\"" + lastCheckedAt + "\",",
        host, host);
  }

  /** A domain status with the TXT and certificate CNAME records in the given statuses. */
  public static String status(String host, String verification, String tls, String txt,
      String certificate) {
    return """
        {"domain":"%1$s","checkedAt":"2026-10-29T09:00:00Z","verification":"%4$s",
         "tls":"%5$s","records":[
          {"purpose":"OWNERSHIP","type":"TXT","name":"_streamx-verification.%1$s",
           "expected":["streamx-domain-verification=b3Jn"],"status":"%2$s","found":[]},
          {"purpose":"CERTIFICATE","type":"CNAME","name":"_acme-challenge.%1$s",
           "expected":["0a1b2c3d.acme.example.net"],"status":"%3$s","found":%6$s}]}
        """.formatted(host, txt, certificate, verification, tls,
        "MISMATCH".equals(certificate) ? "[\"old.acme.example.org\"]" : "[]");
  }

  public static String list(String... domains) {
    return "{\"domains\":[" + String.join(",", domains) + "]}";
  }

  private synchronized void route(HttpExchange exchange) throws IOException {
    String method = exchange.getRequestMethod();
    String path = exchange.getRequestURI().getPath();
    String body = new String(readBody(exchange), StandardCharsets.UTF_8);
    requests.add(method + " " + path);
    requestBodies.add(body);

    if (forcedStatus != 0) {
      respond(exchange, forcedStatus, forcedBody);
    } else if (forcedPutStatus != 0 && "PUT".equals(method)) {
      respond(exchange, forcedPutStatus, forcedBody);
    } else if (path.endsWith("/status")) {
      String answer = queuedChecks.size() > 1 ? queuedChecks.poll() : queuedChecks.peek();
      respond(exchange, 200, answer == null ? "" : answer);
    } else if ("POST".equals(method) && path.endsWith("/verify")) {
      String host = path.replaceAll(".*/domains/", "").replace("/verify", "");
      respond(exchange, 200, domain(host, "PENDING", null, null));
    } else if ("PUT".equals(method)) {
      // The body holds hostnames by ID; each saved domain keeps the ID it was sent with.
      Map<String, String> next = new LinkedHashMap<>();
      Iterator<Map.Entry<String, JsonNode>> entries =
          MAPPER.readTree(body).path("domains").fields();
      while (entries.hasNext()) {
        Map.Entry<String, JsonNode> entry = entries.next();
        String host = entry.getValue().asText();
        next.put(host, withId(applied.getOrDefault(host, domain(host, "PENDING", null, null)),
            entry.getKey()));
      }
      saved = next;
      readsUntilApplied = applyAfterReads;
      if (readsUntilApplied == 0) {
        apply();
      }
      respond(exchange, 200, list(next.values().toArray(String[]::new)));
    } else {
      if (saved != null && readsUntilApplied > 0 && --readsUntilApplied == 0) {
        apply();
      }
      String answer = queuedLists.isEmpty()
          ? list(applied.values().toArray(String[]::new)) : queuedLists.poll();
      respond(exchange, 200, answer);
    }
  }

  private void apply() {
    applied.clear();
    applied.putAll(saved);
    saved = null;
  }

  private static String withId(String json, String id) throws IOException {
    ObjectNode domain = (ObjectNode) MAPPER.readTree(json);
    domain.put("id", id);
    return MAPPER.writeValueAsString(domain);
  }

  private static String field(String json, String name) {
    try {
      return MAPPER.readTree(json).path(name).asText();
    } catch (IOException e) {
      throw new IllegalArgumentException(json, e);
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
