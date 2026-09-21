package com.streamx.cli.auth;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.auth.OidcClient.Endpoints;
import com.streamx.cli.auth.OidcClient.Response;
import com.streamx.cli.framework.CliException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class OidcAuthCodeFlow {
  private static final String LOOPBACK_HOST = "127.0.0.1";
  private static final String CALLBACK_PATH = "/callback";
  private static final Duration LOGIN_TIMEOUT = Duration.ofMinutes(5);

  private final OidcClient client;
  private final BrowserLauncher browserLauncher;

  public OidcAuthCodeFlow(OidcClient client) {
    this(client, OidcAuthCodeFlow::openInBrowser);
  }

  public OidcAuthCodeFlow(OidcClient client, BrowserLauncher browserLauncher) {
    this.client = client;
    this.browserLauncher = browserLauncher;
  }

  public interface BrowserLauncher {
    void open(String url) throws IOException;
  }

  public Credentials login(Endpoints endpoints) {
    return login(endpoints, Scopes.LOGIN);
  }

  public Credentials login(Endpoints endpoints, String scope) {
    if (endpoints.authorizationEndpoint() == null) {
      throw new CliException(msg.authCodeFlowUnsupported());
    }

    Pkce pkce = Pkce.generate();
    String state = Pkce.randomUrlSafe(32);

    LoopbackReceiver receiver = new LoopbackReceiver(state);
    receiver.start();
    try {
      String authUrl = authorizationUrl(endpoints, receiver.redirectUri(), pkce, state, scope);
      try {
        browserLauncher.open(authUrl);
      } catch (IOException e) {
        throw new BrowserUnavailableException();
      }

      System.err.println(msg.authLoginOpeningBrowser());
      System.err.println("  " + authUrl);

      String code = receiver.awaitCode();
      return exchangeCode(endpoints, code, pkce.verifier(), receiver.redirectUri());
    } finally {
      receiver.stop();
    }
  }

  public static class BrowserUnavailableException extends RuntimeException {
  }

  private String authorizationUrl(
      Endpoints endpoints, String redirectUri, Pkce pkce, String state, String scope) {
    Map<String, String> params = Map.of(
        "client_id", client.clientId(),
        "response_type", "code",
        "scope", scope,
        "redirect_uri", redirectUri,
        "state", state,
        "code_challenge", pkce.challenge(),
        "code_challenge_method", Pkce.METHOD
    );
    StringBuilder query = new StringBuilder();
    for (Map.Entry<String, String> entry : params.entrySet()) {
      if (!query.isEmpty()) {
        query.append('&');
      }
      query.append(entry.getKey()).append('=')
          .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
    }
    return endpoints.authorizationEndpoint() + "?" + query;
  }

  private Credentials exchangeCode(
      Endpoints endpoints, String code, String codeVerifier, String redirectUri) {
    String url = endpoints.tokenEndpoint();
    Response response = client.exchange(OidcClient.postForm(url, Map.of(
        "grant_type", "authorization_code",
        "code", code,
        "client_id", client.clientId(),
        "redirect_uri", redirectUri,
        "code_verifier", codeVerifier
    )), url);

    if (!response.isSuccess()) {
      String error = response.body().path("error").asText(null);
      throw new CliException(
          error != null ? msg.authLoginFailed(error)
              : msg.authRequestFailedWithStatus(url, response.statusCode()));
    }
    return client.toCredentials(response.body(), null);
  }

  private static void openInBrowser(String url) throws IOException {
    String os = System.getProperty("os.name", "").toLowerCase();
    ProcessBuilder builder;
    if (os.contains("mac")) {
      builder = new ProcessBuilder("open", url);
    } else if (os.contains("win")) {
      builder = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
    } else {
      builder = new ProcessBuilder("xdg-open", url);
    }
    builder.redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start();
  }

  private static final class LoopbackReceiver {
    private final String expectedState;
    private final BlockingQueue<Result> result = new ArrayBlockingQueue<>(1);
    private HttpServer server;

    private LoopbackReceiver(String expectedState) {
      this.expectedState = expectedState;
    }

    private record Result(String code, String error) {
    }

    private void start() {
      try {
        server = HttpServer.create(new InetSocketAddress(LOOPBACK_HOST, 0), 0);
      } catch (IOException e) {
        throw new CliException(msg.authLoopbackFailed(e.getMessage()), e);
      }
      server.createContext(CALLBACK_PATH, this::handle);
      server.start();
    }

    private String redirectUri() {
      return "http://" + LOOPBACK_HOST + ":" + server.getAddress().getPort() + CALLBACK_PATH;
    }

    private void handle(HttpExchange exchange) throws IOException {
      Map<String, String> query = parseQuery(exchange.getRequestURI());
      String state = query.get("state");

      // Validate state before anything else (RFC 6749 §4.1.2.1), on both success and error.
      if (!expectedState.equals(state)) {
        writeHtml(exchange, msg.authLoopbackDenied());
        return;
      }

      String error = query.get("error");
      String code = query.get("code");
      String body;
      if (error != null) {
        body = msg.authLoopbackDenied();
        result.offer(new Result(null, error));
      } else if (code != null) {
        body = msg.authLoopbackSuccess();
        result.offer(new Result(code, null));
      } else {
        body = msg.authLoopbackDenied();
        result.offer(new Result(null, "invalid_request"));
      }
      writeHtml(exchange, body);
    }

    private static void writeHtml(HttpExchange exchange, String body) throws IOException {
      byte[] bytes = ("<html><body>" + body + "</body></html>").getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
      exchange.sendResponseHeaders(200, bytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(bytes);
      }
    }

    private String awaitCode() {
      Result received;
      try {
        received = result.poll(LOGIN_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new CliException(msg.authLoginInterrupted(), e);
      }
      if (received == null) {
        throw new CliException(msg.authLoginExpired());
      }
      if (received.error() != null) {
        throw new CliException("access_denied".equals(received.error())
            ? msg.authLoginDenied()
            : msg.authLoginFailed(received.error()));
      }
      return received.code();
    }

    private void stop() {
      if (server != null) {
        server.stop(0);
      }
    }

    private static Map<String, String> parseQuery(URI uri) {
      Map<String, String> params = new java.util.HashMap<>();
      String query = uri.getRawQuery();
      if (query == null) {
        return params;
      }
      for (String pair : query.split("&")) {
        int eq = pair.indexOf('=');
        if (eq > 0) {
          params.put(
              java.net.URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
              java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
      }
      return params;
    }
  }
}
