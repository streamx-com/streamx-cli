package com.streamx.cli.platform;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.Urls;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class PlatformClients implements AutoCloseable {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final long TIMEOUT_MS = 30_000;
  private static final long COMPLETION_TIMEOUT_MS = 3_000;

  private final URI baseUri;
  private final boolean insecure;
  private final long timeoutMs;
  private final List<AutoCloseable> built = new ArrayList<>();

  PlatformClients(String url, boolean insecure, long timeoutMs) {
    if (Urls.isCleartextRemote(url)) {
      throw new CliException(msg.platformCleartextHttpBlocked(url));
    }
    this.baseUri = URI.create(url.endsWith("/") ? url.substring(0, url.length() - 1) : url);
    this.insecure = insecure;
    this.timeoutMs = timeoutMs;
  }

  public static PlatformClients fromConfig() {
    return create(TIMEOUT_MS);
  }

  public static PlatformClients completion() {
    return create(COMPLETION_TIMEOUT_MS);
  }

  private static PlatformClients create(long timeoutMs) {
    PlatformConfig config = PlatformConfig.load();
    String url = config.url()
        .filter(value -> !value.isBlank())
        .orElseThrow(() -> new CliException(
            msg.platformUrlNotConfigured(PlatformConfig.STREAMX_PLATFORM_URL)));
    return new PlatformClients(url, config.insecure(), timeoutMs);
  }

  public <T> T api(Class<T> apiType) {
    QuarkusRestClientBuilder builder = QuarkusRestClientBuilder.newBuilder()
        .baseUri(baseUri)
        .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .register(AuthHeaderFilter.class);
    if (insecure) {
      builder.trustAll(true).verifyHost(false);
    }
    T client = builder.build(apiType);
    if (client instanceof AutoCloseable closeable) {
      built.add(closeable);
    }
    return client;
  }

  @Override
  public void close() {
    for (AutoCloseable client : built) {
      try {
        client.close();
      } catch (Exception ignored) {
        continue;
      }
    }
    built.clear();
  }

  public <T> T call(Supplier<Response> operation, Class<T> type) {
    Response response = invoke(operation);
    // A personal access token cannot be refreshed, so retrying would just re-send the same
    // rejected credential; only a login session is worth a second attempt.
    if (response.getStatus() == 401 && !AccessTokens.usingPlatformToken()) {
      AccessTokens.forceRefresh();
      response = invoke(operation);
    }
    return handle(response, type);
  }

  public void call(Supplier<Response> operation) {
    call(operation, null);
  }

  public <T> List<T> callList(Supplier<Response> operation, Class<T> type) {
    JsonNode array = call(operation, JsonNode.class);
    List<T> items = new ArrayList<>();
    if (array != null && array.isArray()) {
      for (JsonNode node : array) {
        items.add(MAPPER.convertValue(node, type));
      }
    }
    return items;
  }

  private Response invoke(Supplier<Response> operation) {
    try {
      return operation.get();
    } catch (WebApplicationException errorStatus) {
      return errorStatus.getResponse();
    } catch (RuntimeException failure) {
      throw asCliException(failure);
    }
  }

  private <T> T handle(Response response, Class<T> type) {
    int status = response.getStatus();
    if (status == 404) {
      throw new NotFoundException(msg.platformNotFound());
    }
    if (status < 200 || status >= 300) {
      String body = response.hasEntity() ? response.readEntity(String.class) : "";
      throw new CliException(errorMessage(status, body));
    }
    if (type == null || !response.hasEntity()) {
      return null;
    }
    return response.readEntity(type);
  }

  private CliException asCliException(RuntimeException failure) {
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      if (cause instanceof CliException cli) {
        return cli;
      }
    }
    return new CliException(
        msg.platformRequestFailed(baseUri.toString(), failure.getMessage()), failure);
  }

  private String errorMessage(int status, String body) {
    String detail = extractErrorMessage(body);
    return switch (status) {
      // Keep the server's explanation (e.g. "personal access tokens cannot manage tokens"),
      // and point at the credential actually in use rather than always at 'auth login'.
      case 401 -> withDetail(AccessTokens.usingPlatformToken()
          ? msg.platformTokenUnauthorized() : msg.platformUnauthorized(), detail);
      case 403 -> withDetail(msg.platformAccessDenied(), detail);
      default -> detail == null
          ? msg.platformRequestFailedWithStatus(baseUri.toString(), status)
          : msg.platformRequestRejected(status, detail);
    };
  }

  private static String withDetail(String message, String detail) {
    return detail == null || detail.isBlank() ? message : message + "\n  " + detail;
  }

  private static String extractErrorMessage(String body) {
    if (body == null || body.isBlank()) {
      return null;
    }
    try {
      JsonNode node = MAPPER.readTree(body);
      String message = node.path("errorMessage").asText(null);
      StringBuilder detail = new StringBuilder(message == null ? "" : message);
      for (JsonNode violation : node.path("violations")) {
        detail.append("\n  ")
            .append(violation.path("field").asText(""))
            .append(": ")
            .append(violation.path("message").asText(""));
      }
      return detail.isEmpty() ? null : detail.toString();
    } catch (com.fasterxml.jackson.core.JsonProcessingException notJson) {
      return null;
    }
  }

  public static class NotFoundException extends CliException {
    public NotFoundException(String message) {
      super(message);
    }
  }
}
