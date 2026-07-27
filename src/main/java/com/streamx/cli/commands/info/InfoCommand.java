package com.streamx.cli.commands.info;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.auth.AuthConfig;
import com.streamx.cli.auth.Credentials;
import com.streamx.cli.auth.CredentialsStore;
import com.streamx.cli.auth.OidcClient;
import com.streamx.cli.commands.info.InfoResult.Probe;
import com.streamx.cli.commands.info.InfoResult.Setting;
import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.TextTable;
import com.streamx.cli.framework.Urls;
import com.streamx.cli.ingestion.IngestionClientConfig;
import com.streamx.cli.platform.AccessTokens;
import com.streamx.cli.platform.PlatformConfig;
import com.streamx.cli.platform.PlatformContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import picocli.CommandLine;

@CommandLine.Command(
    name = "info",
    header = "Show CLI, profile and connectivity diagnostics",
    description = "Reports the CLI version, the active profile and where it came from, the "
        + "effective endpoint settings, the stored login, and probes the configured endpoints. "
        + "Useful as the first thing to share when something does not work."
)
public class InfoCommand extends AbstractCommand<InfoResult> {

  private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(3);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String UP = "UP";
  private static final String DEGRADED = "DEGRADED";
  private static final String DOWN = "DOWN";
  private static final String TLS_ERROR = "TLS ERROR";
  private static final String AUTH_REJECTED = "AUTH REJECTED";

  @CommandLine.Option(
      names = "--check",
      description = "Exit with code 1 unless every configured endpoint probes healthy (UP)"
  )
  public boolean check;

  /** Diagnostics must keep working when the selected profile is broken or missing. */
  @Override
  public boolean needsProfile() {
    return false;
  }

  @Override
  public CommandResult<InfoResult> runCommand() {
    List<String> warnings = new ArrayList<>();

    InfoResult.Cli cli = new InfoResult.Cli(
        cliVersion(),
        runtime(),
        StreamxHome.getStreamxHome().toString(),
        StreamxHome.getStreamxHomeSource());

    String active = null;
    try {
      active = StreamxHome.getActiveProfile();
    } catch (CliException e) {
      warnings.add(e.getMessage());
    }
    boolean exists = active != null && StreamxHome.profileExists(active);
    InfoResult.Profile profile = new InfoResult.Profile(
        active,
        StreamxHome.getActiveProfileSource(),
        exists,
        active == null ? null : StreamxHome.getConfigDirOf(active)
            .resolve("application.properties").toString(),
        quiet(PlatformContext::effectiveOrg),
        quiet(PlatformContext::effectiveOrgSource),
        quiet(PlatformContext::effectiveProject),
        quiet(PlatformContext::effectiveProjectSource));
    if (active != null && !exists) {
      warnings.add("Profile '" + active + "' does not exist yet");
    }

    Properties fileSettings = exists ? loadSettings(active) : new Properties();
    List<Setting> settings = collectSettings(fileSettings, warnings);

    InfoResult.Login login = describeLogin(fileSettings, warnings);

    List<Probe> connectivity = probeEndpoints(fileSettings);

    InfoResult result =
        new InfoResult(cli, profile, settings, login, connectivity, warnings);
    CommandResult<InfoResult> commandResult = new CommandResult<>(result);
    if (check && connectivity.stream().anyMatch(p -> !UP.equals(p.status()))) {
      commandResult.setExitCodeOverride(1);
    }
    return commandResult;
  }

  // ---- report assembly -----------------------------------------------------

  private static List<Setting> collectSettings(Properties file, List<String> warnings) {
    List<Setting> settings = new ArrayList<>();
    settings.add(describeSetting(file, AuthConfig.STREAMX_AUTH_SERVER_URL, null));
    settings.add(describeSetting(file, AuthConfig.STREAMX_AUTH_REALM, AuthConfig.DEFAULT_REALM));
    settings.add(describeSetting(
        file, AuthConfig.STREAMX_AUTH_CLIENT_ID, AuthConfig.DEFAULT_CLIENT_ID));
    settings.add(describeSetting(file, AuthConfig.STREAMX_AUTH_INSECURE, "false"));
    settings.add(describeSetting(file, PlatformConfig.STREAMX_PLATFORM_URL, null));
    settings.add(describeSetting(file, PlatformConfig.STREAMX_PLATFORM_INSECURE, "false"));
    settings.add(describeSetting(file, IngestionClientConfig.STREAMX_INGESTION_URL, null));
    settings.add(describeSetting(file, IngestionClientConfig.STREAMX_INGESTION_INSECURE, "false"));

    if (effective(file, AuthConfig.STREAMX_AUTH_SERVER_URL) == null) {
      warnings.add("Auth server is not configured - run: streamx profile configure");
    }
    for (String urlKey : List.of(AuthConfig.STREAMX_AUTH_SERVER_URL,
        PlatformConfig.STREAMX_PLATFORM_URL, IngestionClientConfig.STREAMX_INGESTION_URL)) {
      String url = effective(file, urlKey);
      if (url != null && url.startsWith("http://")) {
        warnings.add(urlKey + " uses cleartext http - credentials may be exposed");
      }
    }
    return settings;
  }

  private static Setting describeSetting(Properties file, String key, String defaultValue) {
    String fileValue = file.getProperty(key);
    String systemValue = System.getProperty(key);
    if (systemValue != null && !systemValue.equals(fileValue)) {
      return new Setting(key, systemValue, "system property override");
    }
    if (fileValue != null && !fileValue.isBlank()) {
      return new Setting(key, fileValue, "profile");
    }
    if (defaultValue != null) {
      return new Setting(key, defaultValue, "default");
    }
    return new Setting(key, null, "not set");
  }

  private static String effective(Properties file, String key) {
    String systemValue = System.getProperty(key);
    if (systemValue != null && !systemValue.isBlank()) {
      return systemValue;
    }
    String fileValue = file.getProperty(key);
    return fileValue == null || fileValue.isBlank() ? null : fileValue;
  }

  private InfoResult.Login describeLogin(Properties file, List<String> warnings) {
    if (AccessTokens.usingPlatformToken()) {
      // The env credential outranks any stored session; a token is opaque and never expires.
      return new InfoResult.Login("authenticated with a personal access token", null, null, null);
    }
    Optional<Credentials> credentials;
    try {
      credentials = CredentialsStore.load();
    } catch (RuntimeException e) {
      warnings.add("Stored login is unreadable: " + e.getMessage());
      return new InfoResult.Login("unreadable", null, null, null);
    }
    if (credentials.isEmpty()) {
      return new InfoResult.Login("not logged in", null, null, null);
    }
    Credentials creds = credentials.get();
    boolean expired = creds.expiresAt() != null && creds.expiresAt().isBefore(Instant.now());
    String state = expired ? "expired (a refresh is attempted on use)" : "logged in";
    String user = usernameFromJwt(creds.accessToken());

    String serverUrl = effective(file, AuthConfig.STREAMX_AUTH_SERVER_URL);
    String realm = Optional.ofNullable(effective(file, AuthConfig.STREAMX_AUTH_REALM))
        .orElse(AuthConfig.DEFAULT_REALM);
    if (serverUrl != null && creds.issuerUrl() != null) {
      String expected = OidcClient.issuerUrl(serverUrl, realm);
      if (!expected.equals(creds.issuerUrl())) {
        warnings.add("Stored login belongs to " + creds.issuerUrl()
            + " but this profile is configured for " + expected
            + " - run: streamx auth login");
      }
    }
    return new InfoResult.Login(
        state,
        user,
        creds.expiresAt() == null ? null : creds.expiresAt().toString(),
        creds.issuerUrl());
  }

  /** Display-only decode of the JWT payload; the token is never verified here. */
  private static String usernameFromJwt(String accessToken) {
    if (accessToken == null) {
      return null;
    }
    String[] parts = accessToken.split("\\.");
    if (parts.length < 2) {
      return null;
    }
    try {
      JsonNode payload =
          MAPPER.readTree(Base64.getUrlDecoder().decode(parts[1]));
      String username = payload.path("preferred_username").asText(null);
      return username != null ? username : payload.path("email").asText(null);
    } catch (IOException | IllegalArgumentException e) {
      return null;
    }
  }

  // ---- endpoint probes -----------------------------------------------------

  private List<Probe> probeEndpoints(Properties file) {
    String authUrl = effective(file, AuthConfig.STREAMX_AUTH_SERVER_URL);
    String realm = Optional.ofNullable(effective(file, AuthConfig.STREAMX_AUTH_REALM))
        .orElse(AuthConfig.DEFAULT_REALM);
    boolean authInsecure = Boolean.parseBoolean(effective(file, AuthConfig.STREAMX_AUTH_INSECURE));
    String platformUrl = effective(file, PlatformConfig.STREAMX_PLATFORM_URL);
    boolean platformInsecure =
        Boolean.parseBoolean(effective(file, PlatformConfig.STREAMX_PLATFORM_INSECURE));
    Optional<Credentials> credentials;
    try {
      credentials = CredentialsStore.load();
    } catch (RuntimeException unreadable) {
      credentials = Optional.empty();
    }
    String ingestionUrl = effective(file, IngestionClientConfig.STREAMX_INGESTION_URL);
    boolean ingestionInsecure =
        Boolean.parseBoolean(effective(file, IngestionClientConfig.STREAMX_INGESTION_INSECURE));

    List<Callable<Probe>> probes = new ArrayList<>();
    if (authUrl != null) {
      String discovery = OidcClient.issuerUrl(authUrl, realm) + "/.well-known/openid-configuration";
      probes.add(() -> probeAuth(discovery, authInsecure));
    }
    if (platformUrl != null) {
      String base = platformUrl.replaceAll("/+$", "");
      probes.add(() -> probePlatform(base, platformInsecure));
    }
    if (ingestionUrl != null) {
      String base = ingestionUrl.replaceAll("/+$", "");
      probes.add(() -> probeHealth("ingestion", base, ingestionInsecure));
    }
    if (platformUrl != null && (credentials.isPresent() || AccessTokens.usingPlatformToken())) {
      String base = platformUrl.replaceAll("/+$", "");
      if (Urls.isCleartextRemote(base)) {
        probes.add(() -> new Probe("api (authenticated)", base, DEGRADED, null,
            "not probed: refusing to send a credential over cleartext http"));
      } else {
        // AccessTokens prefers the env personal access token, then the stored session.
        String token = quiet(AccessTokens::current);
        if (token == null) {
          probes.add(() -> new Probe("api (authenticated)", base, DEGRADED, null,
              "not probed: no usable credential - run: streamx auth login"));
        } else {
          probes.add(() -> probeAuthenticatedApi(base, platformInsecure, token));
        }
      }
    }
    if (probes.isEmpty()) {
      return List.of();
    }

    ExecutorService pool = Executors.newFixedThreadPool(probes.size());
    try {
      List<Future<Probe>> futures = pool.invokeAll(
          probes, PROBE_TIMEOUT.toSeconds() + 3, TimeUnit.SECONDS);
      List<Probe> results = new ArrayList<>();
      for (Future<Probe> future : futures) {
        try {
          results.add(future.get());
        } catch (Exception e) {
          results.add(new Probe("unknown", null, DOWN, null, "probe did not finish"));
        }
      }
      return results;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return List.of();
    } finally {
      pool.shutdownNow();
    }
  }

  private static Probe probeAuth(String discoveryUrl, boolean insecure) {
    HttpProbe probe = get(discoveryUrl, insecure, null);
    if (probe.status == 200) {
      return new Probe("auth", discoveryUrl, UP, probe.latencyMs, "discovery document served");
    }
    if (probe.status == 404) {
      return new Probe("auth", discoveryUrl, DOWN, probe.latencyMs,
          "server responds but the realm does not exist");
    }
    return failure("auth", discoveryUrl, probe);
  }

  private Probe probePlatform(String baseUrl, boolean insecure) {
    Probe health = probeHealth("platform", baseUrl, insecure);
    if (UP.equals(health.status()) || TLS_ERROR.equals(health.status())) {
      return health;
    }
    String apiUrl = baseUrl + "/api/v1/organizations";
    HttpProbe fallback = get(apiUrl, insecure, null);
    boolean alive = fallback.status >= 200 && fallback.status < 400
        || fallback.status == 401 || fallback.status == 403;
    if (alive) {
      return new Probe("platform", apiUrl, UP, fallback.latencyMs,
          "health endpoint unavailable; API answered HTTP " + fallback.status);
    }
    if (fallback.status == 404) {
      return new Probe("platform", apiUrl, DOWN, fallback.latencyMs,
          "answers HTTP 404 - whatever is at this URL is not the StreamX platform API; "
              + "check streamx.platform.url");
    }
    if (fallback.status > 0) {
      return new Probe("platform", apiUrl, DOWN, fallback.latencyMs,
          "HTTP " + fallback.status + " - the endpoint is reachable but the platform "
              + "is not serving");
    }
    return health;
  }

  private static Probe probeHealth(String name, String baseUrl, boolean insecure) {
    String url = baseUrl + "/q/health/ready";
    HttpProbe probe = get(url, insecure, null);
    if (probe.status == 200) {
      boolean up = probe.body != null && probe.body.contains("\"UP\"");
      return new Probe(name, url, up ? UP : DEGRADED, probe.latencyMs,
          up ? "ready" : "health endpoint reports not ready");
    }
    if (probe.status > 0) {
      return new Probe(name, url, DEGRADED, probe.latencyMs, "HTTP " + probe.status);
    }
    return failure(name, url, probe);
  }

  private static Probe probeAuthenticatedApi(String baseUrl, boolean insecure, String token) {
    String url = baseUrl + "/api/v1/organizations";
    HttpProbe probe = get(url, insecure, token);
    if (probe.status == 200) {
      String detail = "token accepted";
      try {
        JsonNode node = MAPPER.readTree(probe.body);
        if (node.isArray()) {
          detail = "token accepted; member of " + node.size() + " organization(s)";
        }
      } catch (IOException expected) {
      }
      return new Probe("api (authenticated)", url, UP, probe.latencyMs, detail);
    }
    if (probe.status == 401 || probe.status == 403) {
      // 'auth login' is impossible in CI; point at the credential actually in use.
      String remedy = AccessTokens.usingPlatformToken()
          ? "check the personal access token in " + AccessTokens.STREAMX_PLATFORM_TOKEN
          : "run: streamx auth login";
      return new Probe("api (authenticated)", url, AUTH_REJECTED, probe.latencyMs,
          "token rejected (HTTP " + probe.status + ") - " + remedy);
    }
    if (probe.status > 0) {
      return new Probe("api (authenticated)", url, DEGRADED, probe.latencyMs,
          "HTTP " + probe.status);
    }
    return failure("api (authenticated)", url, probe);
  }

  /**
   * Failed probes never reached the server, so the elapsed time is reported as time-to-failure
   * in the detail (fast = DNS/refused, ~timeout = blackholed) - not in the latency column.
   */
  private static Probe failure(String name, String url, HttpProbe probe) {
    String after = "failed after " + probe.latencyMs + "ms: ";
    if (probe.tlsError) {
      return new Probe(name, url, TLS_ERROR, null,
          after + probe.error + " - self-signed certs need the matching *.insecure=true setting");
    }
    return new Probe(name, url, DOWN, null, after + probe.error);
  }

  private record HttpProbe(int status, String body, Long latencyMs, String error,
                           boolean tlsError) {
  }

  private static HttpProbe get(String url, boolean insecure, String bearerToken) {
    long start = System.nanoTime();
    try (CloseableHttpClient client = buildHttpClient(insecure, bearerToken == null)) {
      HttpGet request = new HttpGet(url);
      request.setHeader("Accept", "application/json");
      if (bearerToken != null) {
        request.setHeader("Authorization", "Bearer " + bearerToken);
      }
      try (CloseableHttpResponse response = client.execute(request)) {
        long latency = (System.nanoTime() - start) / 1_000_000;
        String body = response.getEntity() == null ? null
            : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        return new HttpProbe(
            response.getStatusLine().getStatusCode(), body, latency, null, false);
      }
    } catch (SSLException e) {
      return new HttpProbe(0, null, elapsedMs(start), rootMessage(e), true);
    } catch (IOException | RuntimeException e) {
      return new HttpProbe(0, null, elapsedMs(start), rootMessage(e), false);
    }
  }

  private static long elapsedMs(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000;
  }

  private static String rootMessage(Throwable throwable) {
    Throwable root = throwable;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    String message = root.getMessage();
    return message == null || message.isBlank() ? root.getClass().getSimpleName() : message;
  }

  private static CloseableHttpClient buildHttpClient(boolean insecure, boolean followRedirects) {
    int timeoutMillis = (int) PROBE_TIMEOUT.toMillis();
    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectTimeout(timeoutMillis)
        .setConnectionRequestTimeout(timeoutMillis)
        .setSocketTimeout(timeoutMillis)
        .build();
    HttpClientBuilder builder = HttpClients.custom()
        .setDefaultRequestConfig(requestConfig)
        .disableAutomaticRetries();
    if (!followRedirects) {
      builder.disableRedirectHandling();
    }
    if (!insecure) {
      return builder.build();
    }
    try {
      SSLContext sslContext = SSLContexts.custom()
          .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
          .build();
      return builder
          .setSSLContext(sslContext)
          .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
          .build();
    } catch (GeneralSecurityException e) {
      throw new CliException(e.getMessage(), e);
    }
  }

  // ---- helpers -------------------------------------------------------------

  /** "native (Substrate VM, JDK x)" in a native image, "JVM (<vm> <version>)" otherwise. */
  private static String runtime() {
    String vm = System.getProperty("java.vm.name", "unknown");
    String version = System.getProperty("java.version", "");
    if ("runtime".equals(System.getProperty("org.graalvm.nativeimage.imagecode"))) {
      return "native (" + vm + ", JDK " + version + ")";
    }
    return "JVM (" + vm + " " + version + ")";
  }

  private static String quiet(java.util.function.Supplier<String> supplier) {
    try {
      return supplier.get();
    } catch (RuntimeException brokenProfile) {
      return null;
    }
  }

  private String cliVersion() {
    try {
      String[] version = new com.streamx.cli.util.VersionProvider().getVersion();
      return version.length > 0 ? version[0] : "unknown";
    } catch (Exception e) {
      return "unknown";
    }
  }

  private static Properties loadSettings(String profile) {
    Properties properties = new Properties();
    Path path = StreamxHome.getConfigDirOf(profile).resolve("application.properties");
    if (!Files.isRegularFile(path)) {
      return properties;
    }
    try (InputStream in = Files.newInputStream(path)) {
      properties.load(in);
    } catch (IOException unreadable) {
      return properties;
    }
    return properties;
  }

  // ---- text rendering ------------------------------------------------------

  @Override
  public String getTextOutput(CommandResult<InfoResult> result) {
    InfoResult info = result.getData();
    StringBuilder sb = new StringBuilder();

    sb.append("CLI\n");
    row(sb, "version", info.cli().version());
    row(sb, "runtime", info.cli().runtime());
    row(sb, "streamx home", info.cli().home() + "  (" + info.cli().homeSource() + ")");

    sb.append("\nProfile\n");
    row(sb, "active", valueOrDash(info.profile().active())
        + "  (" + info.profile().source() + ")");
    row(sb, "exists", info.profile().exists() ? "yes" : "no");
    row(sb, "settings file", valueOrDash(info.profile().settingsFile()));
    row(sb, "current org", valueOrDash(info.profile().currentOrg())
        + (info.profile().currentOrgSource() == null
            ? "" : "  (" + info.profile().currentOrgSource() + ")"));
    row(sb, "current project", valueOrDash(info.profile().currentProject())
        + (info.profile().currentProjectSource() == null
            ? "" : "  (" + info.profile().currentProjectSource() + ")"));

    sb.append("\nSettings\n");
    sb.append(TextTable.render(
        List.of("KEY", "VALUE", "SOURCE"),
        info.settings().stream()
            .map(s -> List.of(s.key(), valueOrDash(s.value()), s.source()))
            .toList()).indent(2));

    sb.append("\nLogin\n");
    row(sb, "state", info.login().state());
    row(sb, "user", valueOrDash(info.login().user()));
    row(sb, "expires", valueOrDash(info.login().expiresAt()));
    row(sb, "issuer", valueOrDash(info.login().issuer()));

    sb.append("\nConnectivity\n");
    if (info.connectivity().isEmpty()) {
      sb.append("  (no endpoints configured)\n");
    } else {
      sb.append(TextTable.render(
          List.of("ENDPOINT", "STATUS", "LATENCY", "DETAIL"),
          info.connectivity().stream()
              .map(p -> List.of(
                  p.name(),
                  p.status(),
                  p.latencyMs() == null ? "-" : p.latencyMs() + "ms",
                  valueOrDash(p.detail())))
              .toList()).indent(2));
    }

    if (!info.warnings().isEmpty()) {
      sb.append("\nWarnings\n");
      for (String warning : info.warnings()) {
        sb.append("  ! ").append(warning).append('\n');
      }
    }
    return sb.toString().stripTrailing();
  }

  private static void row(StringBuilder sb, String key, String value) {
    sb.append("  ").append(String.format("%-16s", key)).append(' ')
        .append(value == null ? "-" : value).append('\n');
  }

  private static String valueOrDash(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }
}
