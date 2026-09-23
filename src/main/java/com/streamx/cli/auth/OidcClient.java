package com.streamx.cli.auth;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.Urls;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

public class OidcClient {
  private static final String DISCOVERY_PATH = "/.well-known/openid-configuration";
  static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String issuerUrl;
  private final String clientId;
  private final boolean insecure;
  private final CloseableHttpClient httpClient;

  public OidcClient(String issuerUrl, String clientId, boolean insecure) {
    if (Urls.isCleartextRemote(issuerUrl)) {
      throw new CliException(msg.authCleartextHttpBlocked(issuerUrl));
    }
    this.issuerUrl = issuerUrl;
    this.clientId = clientId;
    this.insecure = insecure;
    this.httpClient = buildHttpClient(insecure);
  }

  public static String issuerUrl(String serverUrl, String realm) {
    String base = serverUrl.endsWith("/")
        ? serverUrl.substring(0, serverUrl.length() - 1)
        : serverUrl;
    return base + "/realms/" + realm;
  }

  public String clientId() {
    return clientId;
  }

  public record Endpoints(
      String authorizationEndpoint,
      String deviceAuthorizationEndpoint,
      String tokenEndpoint,
      String revocationEndpoint
  ) {
  }

  public Endpoints discover() {
    String url = issuerUrl + DISCOVERY_PATH;
    JsonNode node = send(get(url), url, true);

    String documentIssuer = node.path("issuer").asText(null);
    if (!issuerUrl.equals(documentIssuer)) {
      throw new CliException(msg.authIssuerMismatch(issuerUrl, String.valueOf(documentIssuer)));
    }
    String authorization = node.path("authorization_endpoint").asText(null);
    String device = node.path("device_authorization_endpoint").asText(null);
    String token = node.path("token_endpoint").asText(null);
    String revocation = node.path("revocation_endpoint").asText(null);

    for (String endpoint : new String[] {authorization, device, token, revocation}) {
      if (endpoint != null && Urls.isCleartextRemote(endpoint)) {
        throw new CliException(msg.authCleartextHttpBlocked(endpoint));
      }
    }
    return new Endpoints(
        authorization,
        device,
        token,
        revocation
    );
  }

  public Credentials refresh(String refreshToken) {
    String url = discover().tokenEndpoint();

    Response response = exchange(postForm(url, Map.of(
        "grant_type", "refresh_token",
        "refresh_token", refreshToken,
        "client_id", clientId
    )), url);

    if (!response.isSuccess()) {
      String error = response.body().path("error").asText(null);
      // Only invalid_grant means the refresh token is dead; other errors keep the session.
      if ("invalid_grant".equals(error)) {
        throw new CliException(msg.authSessionExpired());
      }
      if (error != null) {
        String detail = response.body().path("error_description").asText(error);
        throw new CliException(msg.authTokenRequestRejected(response.statusCode(), detail));
      }
      throw new CliException(msg.authRequestFailedWithStatus(url, response.statusCode()));
    }
    return toCredentials(response.body(), refreshToken);
  }

  public void revoke(String refreshToken) {
    Endpoints endpoints = discover();
    String url = endpoints.revocationEndpoint();
    if (url == null) {
      throw new CliException(msg.authRevocationUnsupported());
    }
    send(postForm(url, Map.of(
        "client_id", clientId,
        "token", refreshToken,
        "token_type_hint", "refresh_token"
    )), url, true);
  }

  public void revokeQuietly(String refreshToken) {
    try {
      Endpoints endpoints = discover();
      if (endpoints.revocationEndpoint() == null) {
        return;
      }
      send(postForm(endpoints.revocationEndpoint(), Map.of(
          "client_id", clientId,
          "token", refreshToken,
          "token_type_hint", "refresh_token"
      )), endpoints.revocationEndpoint(), false);
    } catch (RuntimeException expected) {
    }
  }

  Credentials toCredentials(JsonNode node, String fallbackRefreshToken) {
    String accessToken = node.path("access_token").asText(null);
    if (accessToken == null || accessToken.isBlank()) {
      throw new CliException(msg.authTokenResponseIncomplete());
    }
    return new Credentials(
        accessToken,
        node.path("refresh_token").asText(fallbackRefreshToken),
        Instant.now().plusSeconds(node.path("expires_in").asLong(0)),
        issuerUrl,
        clientId,
        insecure
    );
  }

  static HttpGet get(String url) {
    HttpGet request = new HttpGet(url);
    request.setHeader("Accept", "application/json");
    return request;
  }

  static HttpPost postForm(String url, Map<String, String> form) {
    HttpPost request = new HttpPost(url);
    request.setHeader("Accept", "application/json");

    List<NameValuePair> params = new ArrayList<>();
    for (Map.Entry<String, String> entry : form.entrySet()) {
      params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
    }
    request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
    return request;
  }

  record Response(int statusCode, JsonNode body) {
    boolean isSuccess() {
      return statusCode >= 200 && statusCode < 300;
    }
  }

  JsonNode send(HttpUriRequest request, String url, boolean failOnErrorStatus) {
    Response response = exchange(request, url);
    if (failOnErrorStatus && !response.isSuccess()) {
      throw new CliException(msg.authRequestFailedWithStatus(url, response.statusCode()));
    }
    return response.body();
  }

  Response exchange(HttpUriRequest request, String url) {
    String body;
    int statusCode;
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      statusCode = response.getStatusLine().getStatusCode();
      body = response.getEntity() == null
          ? ""
          : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new CliException(msg.authRequestFailed(url, e.getMessage()), e);
    }

    try {
      JsonNode parsed = body.isBlank() ? MAPPER.createObjectNode() : MAPPER.readTree(body);
      return new Response(statusCode, parsed);
    } catch (IOException e) {
      throw new CliException(msg.authResponseNotJson(url), e);
    }
  }

  private static CloseableHttpClient buildHttpClient(boolean insecure) {
    int timeoutMillis = (int) REQUEST_TIMEOUT.toMillis();
    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectTimeout(timeoutMillis)
        .setConnectionRequestTimeout(timeoutMillis)
        .setSocketTimeout(timeoutMillis)
        .build();

    if (!insecure) {
      return HttpClients.custom()
          .setDefaultRequestConfig(requestConfig)
          .build();
    }

    try {
      SSLContext sslContext = SSLContexts.custom()
          .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
          .build();
      return HttpClients.custom()
          .setDefaultRequestConfig(requestConfig)
          .setSSLContext(sslContext)
          .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
          .build();
    } catch (GeneralSecurityException e) {
      throw new CliException(msg.authInsecureTlsFailed(e.getMessage()), e);
    }
  }
}
