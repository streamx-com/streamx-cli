package com.streamx.cli.commands.project;

import static com.streamx.cli.commands.project.StubDomainsServer.domain;
import static com.streamx.cli.commands.project.StubDomainsServer.list;
import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.commands.project.domains.DomainsSupport;
import com.streamx.cli.platform.AccessTokens;
import com.streamx.cli.platform.PlatformConfig;
import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProjectDomainsCommandIT extends CliBaseIT {

  private static final String ORG = "so-testorg";
  private static final String PROJECT = "so-org-web-a1b2c";
  private static final String DOMAINS_PATH =
      "/api/v1/organizations/" + ORG + "/projects/" + PROJECT + "/domains";
  private static final String GET = "GET " + DOMAINS_PATH;
  private static final String PUT = "PUT " + DOMAINS_PATH;
  private static final String CHECKED_AT = "2026-10-29T09:00:00Z";
  private static final String EDGE_HOSTNAME = PROJECT + ".edge-1.example.net";

  private static final String SHOP_PENDING =
      domain("shop.example.com", "PENDING", "OWNERSHIP_RECORD_NOT_MATCHED", "2026-10-29T08:00:00Z");
  private static final String WWW_VERIFIED =
      domain("www.example.com", "VERIFIED", null, "2026-10-28T10:00:00Z");

  private StubDomainsServer platform;

  @BeforeEach
  void setUp() throws IOException {
    platform = new StubDomainsServer();
    platform.applied(SHOP_PENDING, WWW_VERIFIED);

    Properties properties = new Properties();
    properties.setProperty(PlatformConfig.STREAMX_PLATFORM_URL, platform.getUrl());
    Path configFile = getConfigPath();
    Files.createDirectories(configFile.getParent());
    try (OutputStream out = Files.newOutputStream(configFile)) {
      properties.store(out, null);
    }

    setEnv(AccessTokens.STREAMX_PLATFORM_TOKEN, "test-access-token");
    setEnv(DomainsSupport.POLL_INTERVAL_MS, "10");
  }

  @AfterEach
  void tearDown() {
    if (platform != null) {
      platform.close();
    }
    clearEnv(AccessTokens.STREAMX_PLATFORM_TOKEN);
    clearEnv(DomainsSupport.POLL_INTERVAL_MS);
  }

  private ProcessResult domains(String... args) throws Exception {
    return exec(withProject(args));
  }

  private ProcessResult domainsWithStdin(String stdin, String... args) throws Exception {
    return execWithStdin(stdin, withProject(args));
  }

  private static String[] withProject(String... args) {
    String[] full = new String[args.length + 6];
    full[0] = "project";
    full[1] = "domains";
    System.arraycopy(args, 0, full, 2, args.length);
    full[args.length + 2] = "--org";
    full[args.length + 3] = ORG;
    full[args.length + 4] = "--project";
    full[args.length + 5] = PROJECT;
    return full;
  }

  @Test
  void shouldCompleteDomainIdsAndNamesForExplicitProject() throws Exception {
    ProcessResult result = exec("__complete-domains", ORG, PROJECT);

    result.assertSuccess();
    assertThat(result.stdout().lines().toList())
        .containsExactly("shop", "shop.example.com", "www", "www.example.com");
    assertThat(result.stderr()).isEmpty();
    assertThat(platform.getRequests()).containsExactly(GET);
  }

  @Test
  void shouldCompleteDomainsFromEnvironmentScope() throws Exception {
    setEnv("STREAMX_ORG", ORG);
    setEnv("STREAMX_PROJECT", PROJECT);
    try {
      ProcessResult result = exec("__complete-domains", "", "");

      result.assertSuccess();
      assertThat(result.stdout()).contains("shop.example.com");
      assertThat(platform.getRequests()).containsExactly(GET);
    } finally {
      clearEnv("STREAMX_ORG");
      clearEnv("STREAMX_PROJECT");
    }
  }

  @Test
  void shouldSilentlySkipDomainCompletionWhenPlatformIsUnavailable() throws Exception {
    platform.close();
    platform = null;

    ProcessResult result = exec("__complete-domains", ORG, PROJECT);

    result.assertSuccess();
    assertThat(result.stdout()).isEmpty();
    assertThat(result.stderr()).isEmpty();
  }

  @Test
  void shouldListDomainsWithReasonTexts() throws Exception {
    ProcessResult result = domains("list");

    result.assertSuccess();
    assertThat(result.stdout())
        .contains("DOMAIN ID", "DOMAIN NAME", "DNS CHECKS", "REASON", "NEXT CHECK")
        .containsPattern("shop\\s+shop\\.example\\.com\\s+Checking DNS records\\s+"
            + "Ownership verification record is missing or has the wrong value\\.\\s+"
            + "2026-10-29T08:05:00Z")
        .containsPattern("www\\s+www\\.example\\.com\\s+DNS checks passed\\s+-\\s+-");
    assertThat(platform.getRequests()).containsExactly(GET);
  }

  @Test
  void shouldListDomainsOnlyWhenQuiet() throws Exception {
    ProcessResult result = domains("list", "-q");

    result.assertSuccess();
    assertThat(result.stdout().strip()).isEqualTo("shop.example.com\nwww.example.com");
  }

  @Test
  void shouldKeepRawValuesInJson() throws Exception {
    ProcessResult result = domains("list", "-o", "json");

    result.assertSuccess();
    assertThat(result.stdout())
        .contains("\"failureReason\" : \"OWNERSHIP_RECORD_NOT_MATCHED\"")
        .contains("\"name\" : \"_acme-challenge.shop.example.com\"");
  }

  @Test
  void shouldSayWhenThereAreNoDomains() throws Exception {
    platform.applied();

    ProcessResult result = domains("list");

    result.assertSuccess();
    assertThat(result.stdout()).contains(msg.domainsListEmpty());
  }

  @Test
  void shouldShowOneDomainWithItsRecords() throws Exception {
    ProcessResult result = domains("get", "Shop.Example.com");

    result.assertSuccess();
    assertThat(result.stdout())
        .containsPattern("Domain ID\\s+= shop")
        .containsPattern("DNS checks\\s+= Checking DNS records")
        .containsPattern("Reason\\s+= Ownership verification record is missing or has the "
            + "wrong value\\.")
        .containsPattern("TXT\\s+_streamx-verification\\.shop\\.example\\.com\\s+"
            + "streamx-domain-verification=b3Jn")
        .containsPattern("CNAME\\s+_acme-challenge\\.shop\\.example\\.com\\s+"
            + "0a1b2c3d\\.acme\\.example\\.net");
  }

  @Test
  void shouldRejectGetOfUnknownDomain() throws Exception {
    ProcessResult result = domains("get", "blog.example.com");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.domainNotConfigured("blog.example.com", PROJECT));
  }

  @Test
  void shouldShowOneDomainById() throws Exception {
    ProcessResult result = domains("get", "www");

    result.assertSuccess();
    assertThat(result.stdout())
        .containsPattern("Domain ID\\s+= www")
        .containsPattern("Domain name\\s+= www\\.example\\.com");
  }

  @Test
  void shouldAddOneDomainAndPrintItsRecords() throws Exception {
    ProcessResult result = domains("add", "blog", " Blog.Example.com ");

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(GET, PUT, GET);
    assertThat(platform.getRequestBodies().get(1)).isEqualTo("{\"domains\":{"
        + "\"shop\":\"shop.example.com\",\"www\":\"www.example.com\","
        + "\"blog\":\"blog.example.com\"}}");
    assertThat(result.stdout())
        .containsPattern("Domain ID\\s+= blog")
        .containsPattern("Domain name\\s+= blog\\.example\\.com")
        .contains("_streamx-verification.blog.example.com", "_acme-challenge.blog.example.com")
        .doesNotContain("shop.example.com");
    assertThat(result.stderr())
        .contains(msg.domainAdded("blog.example.com", PROJECT))
        .contains(msg.domainAddNextStep("blog.example.com"));
  }

  @Test
  void shouldAddDomainWithTheGivenId() throws Exception {
    ProcessResult result = domains("add", "news", "blog.example.com");

    result.assertSuccess();
    assertThat(platform.getRequestBodies().get(1)).contains("\"news\":\"blog.example.com\"");
    assertThat(result.stderr()).contains(msg.domainAdded("blog.example.com", PROJECT));
  }

  @Test
  void shouldRequireDomainId() throws Exception {
    ProcessResult result = domains("add");

    result.assertExitCode(2);
    assertThat(result.stderr()).contains("Missing required parameter", "<id>");
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void shouldRequireDomain() throws Exception {
    ProcessResult result = domains("add", "blog");

    result.assertExitCode(2);
    assertThat(result.stderr()).contains("Missing required parameter", "<domain>");
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void shouldRefuseSeveralDomains() throws Exception {
    ProcessResult result = domains("add", "news", "blog.example.com", "news.example.com");

    result.assertExitCode(2);
    assertThat(result.stderr()).contains("Unmatched argument", "news.example.com");
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void shouldRefuseIdThatAnotherDomainUses() throws Exception {
    ProcessResult result = domains("add", "shop", "blog.example.com");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.domainIdTaken("shop", "shop.example.com"));
    assertThat(platform.getRequests()).containsExactly(GET);
  }

  @Test
  void shouldNotWriteWhenDomainIsPresent() throws Exception {
    ProcessResult result = domains("add", "shop", "shop.example.com");

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(GET);
    assertThat(result.stdout()).isBlank();
  }

  @Test
  void shouldNameTheRejectedDomain() throws Exception {
    platform.failPutsWith(400, """
        {"errorCode":400,"errorMessage":"Validation failed","violations":[
          {"field":"domains[blog]","message":"Domain name is too long (at most 231 characters)"}]}
        """);

    ProcessResult result = domains("add", "blog", "blog.example.com");

    result.assertExitCode(1);
    assertThat(result.stderr())
        .contains("Domain 'blog.example.com': Domain name is too long (at most 231 characters)");
  }

  @Test
  void shouldWaitUntilTheChangeApplies() throws Exception {
    platform.applyAfterReads(3);

    ProcessResult result = domains("add", "blog", "blog.example.com");

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(GET, PUT, GET, GET, GET);
    assertThat(result.stderr()).contains(msg.domainAddNextStep("blog.example.com"));
  }

  @Test
  void shouldStillPrintRecordsWhenTheChangeNeverApplies() throws Exception {
    platform.applyAfterReads(-1);

    ProcessResult result = domains("add", "blog", "blog.example.com");

    result.assertExitCode(1);
    assertThat(result.stdout()).contains("_acme-challenge.blog.example.com");
    assertThat(result.stderr()).contains(msg.domainsChangeNotApplied(PROJECT));
  }

  @Test
  void shouldRemovePendingDomainWithoutConfirmation() throws Exception {
    ProcessResult result = domains("remove", "shop.example.com");

    result.assertSuccess();
    assertThat(platform.getRequestBodies().get(1))
        .isEqualTo("{\"domains\":{\"www\":\"www.example.com\"}}");
    assertThat(result.stderr())
        .contains(msg.domainRemoved("shop.example.com", PROJECT))
        .contains(msg.domainRemoveDnsCleanup("streamx-domain-verification=b3Jn",
            "_streamx-verification.shop.example.com", "_acme-challenge.shop.example.com"));
  }

  @Test
  void shouldRemoveVerifiedDomainAfterTypedConfirmation() throws Exception {
    ProcessResult result = domainsWithStdin("www.example.com\n", "remove", "www.example.com");

    result.assertSuccess();
    assertThat(platform.getRequests()).contains(PUT);
    assertThat(result.stderr()).contains(msg.domainRemoveWarning("www.example.com"));
  }

  @Test
  void shouldKeepVerifiedDomainWhenConfirmationDoesNotMatch() throws Exception {
    ProcessResult result = domainsWithStdin("shop.example.com\n", "remove", "www.example.com");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.deleteConfirmMismatch("www.example.com"));
    assertThat(platform.getRequests()).containsExactly(GET);
  }

  @Test
  void shouldRefuseToRemoveVerifiedDomainWithoutInput() throws Exception {
    ProcessResult result = domainsWithStdin("", "remove", "www.example.com");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.deleteConfirmRequired());
    assertThat(platform.getRequests()).containsExactly(GET);
  }

  @Test
  void shouldRemoveVerifiedDomainWithForce() throws Exception {
    ProcessResult result = domains("remove", "www.example.com", "--force");

    result.assertSuccess();
    assertThat(platform.getRequests()).contains(PUT);
  }

  @Test
  void shouldRemoveDomainById() throws Exception {
    ProcessResult result = domains("remove", "shop");

    result.assertSuccess();
    assertThat(platform.getRequestBodies().get(1))
        .isEqualTo("{\"domains\":{\"www\":\"www.example.com\"}}");
    assertThat(result.stderr()).contains(msg.domainRemoved("shop.example.com", PROJECT));
  }

  @Test
  void shouldReportAbsentDomainOnRemove() throws Exception {
    ProcessResult result = domains("remove", "blog.example.com");

    result.assertSuccess();
    assertThat(result.stderr()).contains(msg.domainNotConfigured("blog.example.com", PROJECT));
    assertThat(platform.getRequests()).containsExactly(GET);
  }

  @Test
  void shouldPrintZoneLines() throws Exception {
    ProcessResult result = domains("get-zone-file", "shop.example.com");

    result.assertSuccess();
    assertThat(result.stdout().strip()).isEqualTo(
        "_streamx-verification.shop.example.com. IN TXT \"streamx-domain-verification=b3Jn\"\n"
            + "_acme-challenge.shop.example.com. IN CNAME 0a1b2c3d.acme.example.net.");
    assertThat(platform.getRequests()).containsExactly(GET);
  }

  @Test
  void shouldExportZoneByIdWithoutCheckingReadiness() throws Exception {
    ProcessResult result = domains("get-zone-file", "shop");

    result.assertSuccess();
    assertThat(result.stdout()).contains("_streamx-verification.shop.example.com. IN TXT");
    assertThat(platform.getRequests()).containsExactly(GET);
    assertThat(result.stderr()).isEmpty();
  }

  @Test
  void shouldExportZoneForAllDomainsByDefault() throws Exception {
    ProcessResult result = domains("get-zone-file");

    result.assertSuccess();
    assertThat(result.stdout()).contains(
        "_streamx-verification.shop.example.com. IN TXT",
        "_acme-challenge.shop.example.com. IN CNAME",
        "_streamx-verification.www.example.com. IN TXT",
        "_acme-challenge.www.example.com. IN CNAME");
    assertThat(platform.getRequests()).containsExactly(GET);
    assertThat(result.stderr()).isEmpty();
  }

  @Test
  void shouldRejectZoneWithJsonOutput() throws Exception {
    ProcessResult result = domains("get-zone-file", "shop", "-o", "json");

    result.assertExitCode(2);
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void shouldReturnEmptyZoneForProjectWithoutDomains() throws Exception {
    platform.applied();

    ProcessResult result = domains("get-zone-file");

    result.assertSuccess();
    assertThat(result.stdout()).isEmpty();
    assertThat(result.stderr()).isEmpty();
  }

  @Test
  void shouldRejectZoneWithYamlOutput() throws Exception {
    ProcessResult result = domains("get-zone-file", "--output", "yaml");

    result.assertExitCode(2);
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void shouldNotRequestDnsCheckForPassedDomain() throws Exception {
    ProcessResult result = domains("recheck-dns", "www.example.com");

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(GET);
    assertThat(result.stderr())
        .contains(msg.domainDnsChecksAlreadyPassed("www.example.com"));
  }

  @Test
  void shouldRequestDnsCheckWithoutWaiting() throws Exception {
    ProcessResult result = domains("recheck-dns", "shop.example.com");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly(GET, "POST " + DOMAINS_PATH + "/shop.example.com/verify");
    assertThat(result.stderr()).contains(msg.domainDnsCheckRequested("shop.example.com"));
  }

  @Test
  void shouldRequestDnsCheckById() throws Exception {
    ProcessResult result = domains("recheck-dns", "shop");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly(GET, "POST " + DOMAINS_PATH + "/shop.example.com/verify");
    assertThat(result.stderr()).contains(msg.domainDnsCheckRequested("shop.example.com"));
  }

  @Test
  void shouldIgnoreStaleFailureUntilTheRequestedCheckRuns() throws Exception {
    String failed = domain("shop.example.com", "FAILED", "DNS_CHECK_WINDOW_EXPIRED",
        "2026-10-29T07:00:00Z");
    platform.applied(failed);
    platform.queueLists(
        list(failed),
        list(failed),
        list(domain("shop.example.com", "PENDING", "ACME_DELEGATION_RECORD_MISSING",
            "2026-10-29T08:00:30Z")),
        list(domain("shop.example.com", "VERIFIED", null, "2026-10-29T08:01:00Z")));

    ProcessResult result = domains("recheck-dns", "shop.example.com", "--wait");

    result.assertSuccess();
    assertThat(result.stdout()).containsPattern("DNS checks\\s+= DNS checks passed");
    assertThat(result.stderr())
        .contains(msg.domainDnsChecksPendingUntil(
            msg.domainReasonAcmeDelegationRecordMissing(), "2026-10-29T08:05:00Z"))
        .contains(msg.domainDnsChecksPassed("shop.example.com"));
  }

  @Test
  void shouldFailWhenDnsChecksFail() throws Exception {
    platform.queueLists(
        list(SHOP_PENDING),
        list(domain("shop.example.com", "PENDING", "OWNERSHIP_RECORD_NOT_MATCHED",
            "2026-10-29T08:00:30Z")),
        list(domain("shop.example.com", "FAILED", "DNS_CHECK_WINDOW_EXPIRED",
            "2026-10-29T09:00:00Z")));

    ProcessResult result = domains("recheck-dns", "shop.example.com", "--wait");

    result.assertExitCode(1);
    assertThat(result.stdout()).containsPattern("DNS checks\\s+= DNS checks timed out");
    assertThat(result.stderr()).contains(msg.domainDnsChecksFailed(
        "shop.example.com", msg.domainReasonDnsCheckWindowExpired()));
  }

  @Test
  void shouldIgnoreFailureOfThePreviousWindow() throws Exception {
    // A scheduled check ends the old window between the read and the request.
    platform.queueLists(
        list(SHOP_PENDING),
        list(domain("shop.example.com", "FAILED", "DNS_CHECK_WINDOW_EXPIRED",
            "2026-10-29T08:00:10Z")),
        list(domain("shop.example.com", "PENDING", "OWNERSHIP_RECORD_NOT_MATCHED",
            "2026-10-29T08:00:20Z")),
        list(domain("shop.example.com", "VERIFIED", null, "2026-10-29T08:00:40Z")));

    ProcessResult result = domains("recheck-dns", "shop.example.com", "--wait");

    result.assertSuccess();
    assertThat(result.stderr()).contains(msg.domainDnsChecksPassed("shop.example.com"));
  }

  @Test
  void shouldStopWaitingAtTheTimeout() throws Exception {
    ProcessResult result = domains("recheck-dns", "shop.example.com", "--wait", "--timeout", "1s");

    result.assertExitCode(1);
    assertThat(result.stderr())
        .contains(msg.domainDnsCheckWaitTimedOut("shop.example.com", "1s"));
  }

  @Test
  void shouldStopWaitingWhenTheDomainIsRemoved() throws Exception {
    platform.queueLists(list(SHOP_PENDING), list(WWW_VERIFIED));

    ProcessResult result = domains("recheck-dns", "shop.example.com", "--wait");

    result.assertExitCode(1);
    assertThat(result.stderr())
        .contains(msg.domainRemovedWhileWaiting("shop.example.com", PROJECT));
  }

  @Test
  void shouldRejectInvalidTimeout() throws Exception {
    ProcessResult result = domains(
        "recheck-dns", "shop.example.com", "--wait", "--timeout", "PT1H");

    result.assertExitCode(2);
    assertThat(result.stderr()).contains(msg.invalidDuration("PT1H"));
  }

  @Test
  void shouldShowWhichRecordsAreMissing() throws Exception {
    platform.queueChecks(StubDomainsServer.status("shop.example.com", "PENDING",
        "DNS_CHECKS_NOT_PASSED",
        "DNS_RECORD_MISSING", "DNS_RECORD_VALUE_MISMATCH"));

    ProcessResult result = domains("status", "shop.example.com");

    result.assertExitCode(1);
    assertThat(result.stdout()).isEqualToNormalizingNewlines("""
        Domain ID       = shop
        Domain name     = shop.example.com
        DNS checks      = Checking DNS records
        TLS certificate = DNS checks have not passed
        Last checked    = 2026-10-29T09:00:00Z

        Ownership verification record
          Status         = Record is missing
          Record type    = TXT
          Name           = _streamx-verification.shop.example.com
          Expected value = streamx-domain-verification=b3Jn

        ACME challenge record
          Status         = Record mismatch
          Record type    = CNAME
          Name           = _acme-challenge.shop.example.com
          Expected value = 0a1b2c3d.acme.example.net
          Found value    = old.acme.example.org
        """);
    assertThat(result.stderr()).contains(msg.domainDnsChecksNotPassed("shop.example.com"));
  }

  @Test
  void shouldKeepLongDnsValuesIntactInSeparateRecordSections() throws Exception {
    String target = "a".repeat(63) + ".acme.sandbox.example.net";
    String txt = "streamx-domain-verification=" + "b".repeat(120);
    platform.queueChecks(StubDomainsServer.status("shop.example.com", "PENDING",
        "DNS_CHECKS_NOT_PASSED", "DNS_RECORD_MISSING", "DNS_RECORD_VALUE_MISMATCH")
        .replace("0a1b2c3d.acme.example.net", target)
        .replace("streamx-domain-verification=b3Jn", txt));

    ProcessResult result = domains("status", "shop.example.com");

    result.assertExitCode(1);
    assertThat(result.stdout().lines().toList())
        .contains("  Expected value = " + target, "  Expected value = " + txt,
            "  Found value    = old.acme.example.org");
    assertThat(result.stdout()).doesNotContain("REQUIRED VALUE", "Expected:");
  }

  @Test
  void shouldIgnoreStatusFieldsItDoesNotKnow() throws Exception {
    String status = StubDomainsServer.status("shop.example.com", "PASSED", "TLS_CERTIFICATE_READY",
        "DNS_RECORD_MATCHES", "DNS_RECORD_MATCHES");
    platform.queueChecks(status.substring(0, status.lastIndexOf('}'))
        + ",\"gateways\":[{\"hostname\":\"" + EDGE_HOSTNAME + "\",\"status\":\"SERVED\"}]}");

    ProcessResult result = domains("status", "shop.example.com");

    result.assertSuccess();
    assertThat(result.stdout()).doesNotContain(EDGE_HOSTNAME);
    assertThat(result.stderr())
        .contains(msg.domainTlsCertificateReady("shop.example.com"))
        .doesNotContain("dashboard", "gateway");
  }

  @Test
  void shouldReportMissingRouteWithoutSetupInstructions() throws Exception {
    platform.queueChecks(StubDomainsServer.status("shop.example.com", "PASSED",
        "DOMAIN_NOT_IN_MESH_ROUTES",
        "DNS_RECORD_MATCHES", "DNS_RECORD_MATCHES"));

    ProcessResult result = domains("status", "shop.example.com");

    result.assertExitCode(1);
    assertThat(result.stderr())
        .contains(msg.domainNotInMeshRoutes("shop.example.com"))
        .doesNotContain("mesh/mesh.yaml", "deploy", "gateway");
  }

  @Test
  void shouldExplainHowToRetryFailedDnsChecks() throws Exception {
    platform.queueChecks(StubDomainsServer.status("shop.example.com", "FAILED",
        "DNS_CHECKS_NOT_PASSED",
        "DNS_RECORD_MATCHES", "DNS_RECORD_MATCHES"));

    ProcessResult result = domains("status", "shop.example.com");

    result.assertExitCode(1);
    assertThat(result.stderr())
        .contains(msg.domainDnsChecksRetry("shop.example.com"))
        .doesNotContain(msg.domainDnsChecksNotPassed("shop.example.com"));
  }

  @Test
  void shouldDescribeDnsChecksWithoutAFailureReason() throws Exception {
    platform.queueLists(
        list(SHOP_PENDING),
        list(domain("shop.example.com", "PENDING", null, "2026-10-29T08:00:30Z")),
        list(domain("shop.example.com", "VERIFIED", null, "2026-10-29T08:01:00Z")));

    ProcessResult result = domains("recheck-dns", "shop.example.com", "--wait");

    result.assertSuccess();
    assertThat(result.stderr())
        .contains(msg.domainCheckingDnsUntil("shop.example.com", "2026-10-29T08:05:00Z"))
        .doesNotContain("DNS checks pending: Pending")
        .doesNotContain(msg.domainNotInMeshRoutes("shop.example.com"));
  }

  @Test
  void shouldSucceedWhenTheDomainIsReady() throws Exception {
    platform.queueChecks(StubDomainsServer.status("shop.example.com", "PASSED",
        "TLS_CERTIFICATE_READY",
        "DNS_RECORD_MATCHES", "DNS_RECORD_MATCHES"));

    ProcessResult result = domains("status", "shop.example.com");

    result.assertSuccess();
    assertThat(result.stderr())
        .contains(msg.domainTlsCertificateReady("shop.example.com"))
        .doesNotContain("dashboard", "gateway");
    assertThat(platform.getRequests())
        .containsExactly(GET, "GET " + DOMAINS_PATH + "/shop.example.com/status");
  }

  @Test
  void shouldCheckStatusById() throws Exception {
    platform.queueChecks(StubDomainsServer.status("shop.example.com", "PASSED",
        "TLS_CERTIFICATE_READY",
        "DNS_RECORD_MATCHES", "DNS_RECORD_MATCHES"));

    ProcessResult result = domains("status", "shop");

    result.assertSuccess();
    assertThat(result.stdout())
        .contains("Domain ID       = shop", "Domain name     = shop.example.com");
    assertThat(platform.getRequests())
        .containsExactly(GET, "GET " + DOMAINS_PATH + "/shop.example.com/status");
    assertThat(result.stderr())
        .contains(msg.domainTlsCertificateReady("shop.example.com"))
        .doesNotContain("dashboard", "gateway");
  }

  @Test
  void shouldWatchUntilTheDomainIsReady() throws Exception {
    platform.queueChecks(
        StubDomainsServer.status("shop.example.com", "PENDING", "DNS_CHECKS_NOT_PASSED",
            "DNS_RECORD_MISSING", "DNS_RECORD_MISSING"),
        StubDomainsServer.status("shop.example.com", "PASSED", "TLS_CERTIFICATE_NOT_READY",
            "DNS_RECORD_MATCHES", "DNS_RECORD_MATCHES"),
        StubDomainsServer.status("shop.example.com", "PASSED", "TLS_CERTIFICATE_READY",
            "DNS_RECORD_MATCHES", "DNS_RECORD_MATCHES"));

    ProcessResult result = domains("status", "shop.example.com", "--watch");

    result.assertSuccess();
    // Earlier rounds are progress on stderr; stdout holds the final result only.
    assertThat(result.stdout())
        .contains("Domain ID       = shop", "Domain name     = shop.example.com")
        .contains("TLS certificate = Ready")
        .doesNotContain("Not ready", "Checking DNS records");
    assertThat(result.stderr())
        .contains("Domain ID       = shop", "Domain name     = shop.example.com")
        .contains("DNS checks      = Checking DNS records")
        .contains("TLS certificate = DNS checks have not passed")
        .contains("TLS certificate = Not ready")
        .contains(msg.domainTlsCertificateReady("shop.example.com"));
  }

  @Test
  void shouldRejectStatusOfUnknownDomain() throws Exception {
    ProcessResult result = domains("status", "blog.example.com");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.domainNotConfigured("blog.example.com", PROJECT));
  }

  @Test
  void shouldKeepRawStatusValuesInJson() throws Exception {
    platform.queueChecks(StubDomainsServer.status("shop.example.com", "PASSED",
        "TLS_CERTIFICATE_INVALID", "DNS_RECORD_MATCHES", "DNS_LOOKUP_FAILED"));

    ProcessResult result = domains("status", "shop.example.com", "-o", "json");

    result.assertExitCode(1);
    assertThat(result.stdout())
        .contains("\"id\" : \"shop\"", "\"domain\" : \"shop.example.com\"")
        .contains("\"tlsCertificate\" : \"TLS_CERTIFICATE_INVALID\"")
        .contains("\"purpose\" : \"ACME_DELEGATION\"")
        .contains("\"status\" : \"DNS_LOOKUP_FAILED\"");
    assertThat(result.stderr())
        .contains(msg.domainTlsCertificateInvalid("shop.example.com"));
  }


  @Test
  void shouldExplainNotFound() throws Exception {
    platform.failWith(404, "");

    ProcessResult result = domains("list");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.domainsNotFound(PROJECT));
  }

  @Test
  void shouldExplainUnsupportedPlatform() throws Exception {
    platform.failWith(501, "{\"errorCode\":501,\"errorMessage\":\"Not implemented\"}");

    ProcessResult result = domains("list");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.domainsNotSupported());
  }

  @Test
  void shouldPassConflictsThrough() throws Exception {
    platform.failWith(409, """
        {"errorCode":409,
         "errorMessage":"Failed to update cloud config. Too many concurrent updates"}
        """);

    ProcessResult result = domains("list");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains("Too many concurrent updates");
  }
}
