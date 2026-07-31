package com.streamx.cli.commands.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import javax.net.ssl.SSLContext;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

public class TestHttpClient implements AutoCloseable {
  private final CloseableHttpClient httpClient;

  public TestHttpClient(boolean insecure) {
    if (!insecure) {
      this.httpClient = HttpClients.custom().build();
      return;
    }
    try {
      SSLContext sslContext = SSLContexts.custom()
          .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
          .build();
      this.httpClient = HttpClients.custom()
          .setSSLContext(sslContext)
          .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
          .build();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Cannot build insecure test http client", e);
    }
  }

  public String postForm(String url, String formBody) throws IOException {
    HttpPost request = new HttpPost(url);
    request.setHeader("Content-Type", "application/x-www-form-urlencoded");
    request.setEntity(new StringEntity(formBody, StandardCharsets.UTF_8));

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      return response.getEntity() == null
          ? ""
          : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
    }
  }

  @Override
  public void close() throws IOException {
    httpClient.close();
  }
}
