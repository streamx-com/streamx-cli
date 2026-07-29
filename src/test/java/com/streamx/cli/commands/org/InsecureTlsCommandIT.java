package com.streamx.cli.commands.org;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.platform.PlatformConfig;
import com.streamx.cli.test.CliBaseIT;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Instant;
import java.util.Properties;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class InsecureTlsCommandIT extends CliBaseIT {

  private static final String ORGS = """
      [{"id":"acme","name":"Acme","role":{"name":"owner","displayName":"Owner"},
        "projectsNumber":"1","state":"ACTIVE"}]""";

  private HttpsServer server;
  private String baseUrl;

  @BeforeEach
  void setUp() throws Exception {
    server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setHttpsConfigurator(new HttpsConfigurator(selfSignedContext()));
    server.createContext("/api/v1/organizations", exchange -> {
      byte[] body = ORGS.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(body);
      }
    });
    server.start();
    baseUrl = "https://127.0.0.1:" + server.getAddress().getPort();

    Path credentials = streamxHome.resolve("contexts/default/config/credentials.json");
    Files.createDirectories(credentials.getParent());
    Files.writeString(credentials, """
        {"access_token":"test-access-token","refresh_token":"test-refresh-token",
         "expires_at":%d,"issuer_url":"http://127.0.0.1:1/realms/streamx",
         "client_id":"streamx-cli"}
        """.formatted(Instant.now().plusSeconds(300).getEpochSecond()));
  }

  @AfterEach
  void tearDown() throws IOException {
    if (server != null) {
      server.stop(0);
    }
    Files.deleteIfExists(streamxHome.resolve("contexts/default/config/credentials.json"));
  }

  private void writeConfig(boolean insecure) throws IOException {
    Properties properties = new Properties();
    properties.setProperty(PlatformConfig.STREAMX_PLATFORM_URL, baseUrl);
    properties.setProperty(PlatformConfig.STREAMX_PLATFORM_INSECURE, String.valueOf(insecure));
    try (OutputStream out = Files.newOutputStream(getConfigPath())) {
      properties.store(out, null);
    }
  }

  private static SSLContext selfSignedContext() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    try (var in = InsecureTlsCommandIT.class.getResourceAsStream("/tls/selfsigned.p12")) {
      keyStore.load(in, "changeit".toCharArray());
    }
    KeyManagerFactory keyManagers = KeyManagerFactory.getInstance("SunX509");
    keyManagers.init(keyStore, "changeit".toCharArray());
    SSLContext context = SSLContext.getInstance("TLS");
    context.init(keyManagers.getKeyManagers(), null, null);
    return context;
  }

  @Test
  void trustsSelfSignedCertificateWhenInsecure() throws Exception {
    writeConfig(true);

    ProcessResult result = exec("org", "list", "-q");

    result.assertSuccess();
    assertThat(result.stdout().strip()).isEqualTo("acme");
  }

  @Test
  void rejectsSelfSignedCertificateWhenSecure() throws Exception {
    writeConfig(false);

    ProcessResult result = exec("org", "list", "-q");

    result.assertExitCode(1);
    assertThat(result.stderr()).containsIgnoringCase("SSL");
  }
}
