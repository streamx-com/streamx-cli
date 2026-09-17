package com.streamx.cli.commands.auth;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLContext;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

public class KeycloakDeviceApprover implements AutoCloseable {
  private static final Pattern FORM_ACTION = Pattern.compile("<form[^>]*action=\"([^\"]+)\"");
  private static final Pattern HIDDEN_INPUT = Pattern.compile(
      "<input[^>]*type=\"hidden\"[^>]*name=\"([^\"]+)\"[^>]*value=\"([^\"]*)\"");

  private final CloseableHttpClient httpClient;
  private final HttpClientContext context = HttpClientContext.create();

  public KeycloakDeviceApprover(boolean insecure) {
    this.context.setCookieStore(new BasicCookieStore());

    HttpClientBuilder builder = HttpClients.custom()
        .setRedirectStrategy(new LaxRedirectStrategy());

    if (insecure) {
      try {
        SSLContext sslContext = SSLContexts.custom()
            .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
            .build();
        builder.setSSLContext(sslContext).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
      } catch (GeneralSecurityException e) {
        throw new IllegalStateException("Cannot build insecure test http client", e);
      }
    }
    this.httpClient = builder.build();
  }

  public void approve(String verificationUriComplete, String username, String password)
      throws IOException {
    Page page = get(verificationUriComplete);

    if (page.action == null) {
      throw new IllegalStateException("No form on the device verification page: " + page.summary());
    }

    if (page.action.contains("login-actions") || page.action.contains("authenticate")) {
      Map<String, String> credentials = new LinkedHashMap<>(page.hiddenFields);
      credentials.put("username", username);
      credentials.put("password", password);
      page = post(page.action, credentials);
    }

    if (page.action == null || !page.action.contains("consent")) {
      throw new IllegalStateException(
          "Expected a consent form after login, got: " + page.summary());
    }

    Map<String, String> consent = new LinkedHashMap<>(page.hiddenFields);
    consent.put("accept", "Yes");
    post(page.action, consent);
  }

  private Page get(String url) throws IOException {
    HttpGet request = new HttpGet(url);
    request.setHeader("User-Agent", "Mozilla/5.0");
    return execute(request, url);
  }

  private Page post(String url, Map<String, String> form) throws IOException {
    HttpPost request = new HttpPost(url);
    request.setHeader("User-Agent", "Mozilla/5.0");
    List<NameValuePair> params = new ArrayList<>();
    form.forEach((k, v) -> params.add(new BasicNameValuePair(k, v)));
    request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
    return execute(request, url);
  }

  private Page execute(org.apache.http.client.methods.HttpUriRequest request, String requestUrl)
      throws IOException {
    try (CloseableHttpResponse response = httpClient.execute(request, context)) {
      String body = response.getEntity() == null
          ? ""
          : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
      return new Page(body, resolveCurrentUrl(requestUrl));
    }
  }

  private String resolveCurrentUrl(String requestUrl) {
    List<URI> redirects = context.getRedirectLocations();
    if (redirects != null && !redirects.isEmpty()) {
      return redirects.get(redirects.size() - 1).toString();
    }
    return requestUrl;
  }

  @Override
  public void close() throws IOException {
    httpClient.close();
  }

  private static final class Page {
    private final String body;
    private final String action;
    private final Map<String, String> hiddenFields = new LinkedHashMap<>();

    private Page(String body, String currentUrl) {
      this.body = body;

      Matcher formMatcher = FORM_ACTION.matcher(body);
      this.action = formMatcher.find()
          ? URI.create(currentUrl).resolve(formMatcher.group(1).replace("&amp;", "&")).toString()
          : null;

      Matcher hiddenMatcher = HIDDEN_INPUT.matcher(body);
      while (hiddenMatcher.find()) {
        hiddenFields.put(hiddenMatcher.group(1), hiddenMatcher.group(2));
      }
    }

    private String summary() {
      return body.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").strip();
    }
  }
}
