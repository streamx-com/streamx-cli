package com.streamx.cli.platform;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.Urls;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import javax.net.ssl.SSLContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

public class PlatformApiClient implements AutoCloseable {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final int TIMEOUT_MILLIS = 30_000;

  private final String baseUrl;
  private final CloseableHttpClient httpClient;

  public PlatformApiClient(String baseUrl, boolean insecure) {
    if (Urls.isCleartextRemote(baseUrl)) {
      throw new CliException(msg.platformCleartextHttpBlocked(baseUrl));
    }
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectTimeout(TIMEOUT_MILLIS)
        .setConnectionRequestTimeout(TIMEOUT_MILLIS)
        .setSocketTimeout(TIMEOUT_MILLIS)
        .build();
    HttpClientBuilder builder = HttpClients.custom()
        .setDefaultRequestConfig(requestConfig)
        // Disabled so a redirect can't resend the Authorization header to another host.
        .disableRedirectHandling();

    if (insecure) {
      try {
        SSLContext sslContext = SSLContexts.custom()
            .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
            .build();
        builder.setSSLContext(sslContext).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
      } catch (GeneralSecurityException e) {
        throw new CliException(msg.platformInsecureTlsFailed(e.getMessage()), e);
      }
    }
    this.httpClient = builder.build();
  }

  public static PlatformApiClient fromConfig() {
    PlatformConfig config = PlatformConfig.load();
    String url = config.url()
        .filter(value -> !value.isBlank())
        .orElseThrow(() -> new CliException(
            msg.platformUrlNotConfigured(PlatformConfig.STREAMX_PLATFORM_URL)));
    return new PlatformApiClient(url, config.insecure());
  }

  public JsonNode get(String path) {
    return execute(new HttpGet(baseUrl + path), path);
  }

  public JsonNode postJson(String path, Object body) {
    return executeWithJsonBody(new HttpPost(baseUrl + path), path, body);
  }

  public JsonNode putJson(String path, Object body) {
    return executeWithJsonBody(new HttpPut(baseUrl + path), path, body);
  }

  public JsonNode patchJson(String path, Object body) {
    return executeWithJsonBody(new HttpPatch(baseUrl + path), path, body);
  }

  private JsonNode executeWithJsonBody(
      HttpEntityEnclosingRequestBase request, String path, Object body) {
    request.setHeader("Content-Type", "application/json");
    try {
      request.setEntity(new StringEntity(MAPPER.writeValueAsString(body), StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new CliException(msg.unableToSerializeJson(e.getMessage()), e);
    }
    return execute(request, path);
  }

  public JsonNode delete(String path) {
    return execute(new HttpDelete(baseUrl + path), path);
  }

  public static class NotFoundException extends CliException {
    public NotFoundException(String message) {
      super(message);
    }
  }

  private record Response(int statusCode, String body) {
  }

  private JsonNode execute(HttpUriRequest request, String path) {
    Response response = sendOnce(request, path, AccessTokens.current());

    if (response.statusCode() == 401) {
      response = sendOnce(request, path, AccessTokens.forceRefresh());
    }

    if (response.statusCode() == 404) {
      throw new NotFoundException(msg.platformNotFoundOrForbidden(path));
    }
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new CliException(errorMessage(path, response.statusCode(), response.body()));
    }
    if (response.body().isBlank()) {
      return MAPPER.nullNode();
    }
    try {
      return MAPPER.readTree(response.body());
    } catch (IOException e) {
      throw new CliException(msg.platformResponseNotJson(baseUrl + path), e);
    }
  }

  private Response sendOnce(HttpUriRequest request, String path, String accessToken) {
    request.setHeader("Accept", "application/json");
    request.setHeader("Authorization", "Bearer " + accessToken);

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      return new Response(
          response.getStatusLine().getStatusCode(),
          response.getEntity() == null
              ? ""
              : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new CliException(msg.platformRequestFailed(baseUrl + path, e.getMessage()), e);
    }
  }

  private String errorMessage(String path, int statusCode, String body) {
    String detail = extractErrorMessage(body);
    return switch (statusCode) {
      case 401 -> msg.platformUnauthorized();
      case 403 -> msg.platformForbidden(path);
      case 404 -> msg.platformNotFoundOrForbidden(path);
      default -> detail == null
          ? msg.platformRequestFailedWithStatus(baseUrl + path, statusCode)
          : msg.platformRequestRejected(statusCode, detail);
    };
  }

  private String extractErrorMessage(String body) {
    if (body.isBlank()) {
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
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public void close() {
    try {
      httpClient.close();
    } catch (IOException expected) {
    }
  }
}
