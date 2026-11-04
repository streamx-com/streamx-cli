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
      domain("shop.example.com", "PENDING", "TXT_RECORD_NOT_FOUND", "2026-10-29T08:00:00Z");
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
  void shouldListDomainsWithReasonTexts() throws Exception {
    ProcessResult result = domains("list");

    result.assertSuccess();
    assertThat(result.stdout())
        .contains("ID", "DOMAIN", "STATE", "REASON", "NEXT CHECK")
        .containsPattern("shop\\s+shop\\.example\\.com\\s+PENDING\\s+TXT record not found\\.\\s+"
            + "2026-10-29T08:05:00Z")
        .containsPattern("www\\s+www\\.example\\.com\\s+VERIFIED\\s+-\\s+-");
    assertThat(platform.getRequests()).containsExactly(GET);
  }

  @Test
  void shouldListHostnamesOnlyWhenQuiet() throws Exception {
    ProcessResult result = domains("list", "-q");

    result.assertSuccess();
    assertThat(result.stdout().strip()).isEqualTo("shop.example.com\nwww.example.com");
  }

  @Test
  void shouldKeepRawValuesInJson() throws Exception {
    ProcessResult result = domains("list", "-o", "json");

    result.assertSuccess();
    assertThat(result.stdout())
        .contains("\"failureReason\" : \"TXT_RECORD_NOT_FOUND\"")
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
        .containsPattern("id\\s+= shop")
        .containsPattern("state\\s+= PENDING")
        .containsPattern("reason\\s+= TXT record not found\\.")
        .containsPattern("TXT\\s+_streamx-verification\\.shop\\.example\\.com\\s+"
            + "streamx-domain-verification=b3Jn")
        .containsPattern("CNAME\\s+_acme-challenge\\.shop\\.example\\.com\\s+"
            + "0a1b2c3d\\.acme\\.example\\.net");
  }

  @Test
  void shouldRejectGetOfUnknownHostname() throws Exception {
    ProcessResult result = domains("get", "blog.example.com");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.domainNotConfigured("blog.example.com", PROJECT));
  }

  @Test
  void shouldShowOneDomainById() throws Exception {
    ProcessResult result = domains("get", "www");

    result.assertSuccess();
    assertThat(result.stdout())
        .containsPattern("id\\s+= www")
        .containsPattern("domain\\s+= www\\.example\\.com");
  }

  @Test
  void shouldAddNewHostnamesAndPrintTheirRecords() throws Exception {
    ProcessResult result = domains("add", " Blog.Example.com ", "shop.example.com",
        "blog.example.com");

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(GET, PUT, GET);
    assertThat(platform.getRequestBodies().get(1)).isEqualTo("{\"domains\":{"
        + "\"shop\":\"shop.example.com\",\"www\":\"www.example.com\","
        + "\"blog\":\"blog.example.com\"}}");
    assertThat(result.stdout())
        .contains("_streamx-verification.blog.example.com", "_acme-challenge.blog.example.com")
        .doesNotContain("shop.example.com");
    assertThat(result.stderr())
        .contains(msg.domainAlreadyAdded("shop.example.com", PROJECT))
        .contains(msg.domainAdded("blog.example.com", PROJECT))
        .contains(msg.domainAddNextStep("blog.example.com"));
  }

  @Test
  void shouldAddHostnameWithTheGivenId() throws Exception {
    ProcessResult result = domains("add", "blog.example.com", "--id", "news");

    result.assertSuccess();
    assertThat(platform.getRequestBodies().get(1)).contains("\"news\":\"blog.example.com\"");
    assertThat(result.stderr()).contains(msg.domainAdded("blog.example.com", PROJECT));
  }

  @Test
  void shouldNumberTheDerivedIdWhenItIsTaken() throws Exception {
    ProcessResult result = domains("add", "shop.example.org");

    result.assertSuccess();
    assertThat(platform.getRequestBodies().get(1))
        .contains("\"shop\":\"shop.example.com\"", "\"shop-2\":\"shop.example.org\"");
  }

  @Test
  void shouldRefuseIdForSeveralHostnames() throws Exception {
    ProcessResult result = domains("add", "blog.example.com", "news.example.com", "--id",
        "news");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.domainIdForOneHostname());
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void shouldRefuseIdThatAnotherHostnameUses() throws Exception {
    ProcessResult result = domains("add", "blog.example.com", "--id", "shop");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.domainIdTaken("shop", "shop.example.com"));
    assertThat(platform.getRequests()).containsExactly(GET);
  }

  @Test
  void shouldNotWriteWhenEveryHostnameIsPresent() throws Exception {
    ProcessResult result = domains("add", "shop.example.com");

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(GET);
    assertThat(result.stdout()).isBlank();
  }

  @Test
  void shouldNameTheRejectedHostname() throws Exception {
    platform.failPutsWith(400, """
        {"errorCode":400,"errorMessage":"Validation failed","violations":[
          {"field":"domains[blog]","message":"Domain name is too long (at most 231 characters)"}]}
        """);

    ProcessResult result = domains("add", "blog.example.com");

    result.assertExitCode(1);
    assertThat(result.stderr())
        .contains("blog.example.com: Domain name is too long (at most 231 characters)");
  }

  @Test
  void shouldWaitUntilTheChangeApplies() throws Exception {
    platform.applyAfterReads(3);

    ProcessResult result = domains("add", "blog.example.com");

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(GET, PUT, GET, GET, GET);
    assertThat(result.stderr()).contains(msg.domainAddNextStep("blog.example.com"));
  }

  @Test
  void shouldStillPrintRecordsWhenTheChangeNeverApplies() throws Exception {
    platform.applyAfterReads(-1);

    ProcessResult result = domains("add", "blog.example.com");

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
    assertThat(result.stderr()).contains(msg.domainRemoveVerifiedWarning("www.example.com"));
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
  void shouldPrintRecordsOfAllDomains() throws Exception {
    ProcessResult result = domains("records");

    result.assertSuccess();
    assertThat(result.stdout())
        .contains("_streamx-verification.shop.example.com", "_acme-challenge.www.example.com");
  }

  @Test
  void shouldPrintZoneLines() throws Exception {
    ProcessResult result = domains("records", "shop.example.com", "--zone");

    result.assertSuccess();
    assertThat(result.stdout().strip()).isEqualTo(
        "_streamx-verification.shop.example.com. IN TXT \"streamx-domain-verification=b3Jn\"\n"
            + "_acme-challenge.shop.example.com. IN CNAME 0a1b2c3d.acme.example.net.");
  }

  @Test
  void shouldRejectZoneWithJsonOutput() throws Exception {
    ProcessResult result = domains("records", "--zone", "-o", "json");

    result.assertExitCode(2);
    assertThat(result.stderr()).contains(msg.domainsZoneNeedsText());
    assertThat(platform.getRequests()).isEmpty();
  }

  @Test
  void shouldNotRequestVerificationOfVerifiedDomain() throws Exception {
    ProcessResult result = domains("verify", "www.example.com");

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(GET);
    assertThat(result.stderr())
        .contains(msg.domainAlreadyVerified("www.example.com"))
        .contains(msg.domainVerifiedNextStep("www.example.com"));
  }

  @Test
  void shouldRequestVerificationWithoutWaiting() throws Exception {
    ProcessResult result = domains("verify", "shop.example.com");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly(GET, "POST " + DOMAINS_PATH + "/shop.example.com/verify");
    assertThat(result.stderr()).contains(msg.domainVerificationRequested("shop.example.com"));
  }

  @Test
  void shouldRequestVerificationById() throws Exception {
    ProcessResult result = domains("verify", "shop");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly(GET, "POST " + DOMAINS_PATH + "/shop.example.com/verify");
    assertThat(result.stderr()).contains(msg.domainVerificationRequested("shop.example.com"));
  }

  @Test
  void shouldIgnoreStaleFailureUntilTheRequestedCheckRuns() throws Exception {
    String failed = domain("shop.example.com", "FAILED", "VERIFICATION_WINDOW_EXPIRED",
        "2026-10-29T07:00:00Z");
    platform.applied(failed);
    platform.queueLists(
        list(failed),
        list(failed),
        list(domain("shop.example.com", "PENDING", "CNAME_RECORD_NOT_FOUND",
            "2026-10-29T08:00:30Z")),
        list(domain("shop.example.com", "VERIFIED", null, "2026-10-29T08:01:00Z")));

    ProcessResult result = domains("verify", "shop.example.com", "--wait");

    result.assertSuccess();
    assertThat(result.stdout()).containsPattern("state\\s+= VERIFIED");
    assertThat(result.stderr())
        .contains(msg.domainVerificationPendingUntil(
            msg.domainReasonCnameRecordNotFound(), "2026-10-29T08:05:00Z"))
        .contains(msg.domainVerified("shop.example.com"))
        .contains(msg.domainVerifiedNextStep("shop.example.com"));
  }

  @Test
  void shouldFailWhenVerificationFails() throws Exception {
    platform.queueLists(
        list(SHOP_PENDING),
        list(domain("shop.example.com", "PENDING", "TXT_RECORD_NOT_FOUND",
            "2026-10-29T08:00:30Z")),
        list(domain("shop.example.com", "FAILED", "VERIFICATION_WINDOW_EXPIRED",
            "2026-10-29T09:00:00Z")));

    ProcessResult result = domains("verify", "shop.example.com", "--wait");

    result.assertExitCode(1);
    assertThat(result.stdout()).containsPattern("state\\s+= FAILED");
    assertThat(result.stderr()).contains(msg.domainVerificationFailed(
        "shop.example.com", msg.domainReasonVerificationWindowExpired()));
  }

  @Test
  void shouldIgnoreFailureOfThePreviousWindow() throws Exception {
    // A scheduled check ends the old window between the read and the request.
    platform.queueLists(
        list(SHOP_PENDING),
        list(domain("shop.example.com", "FAILED", "VERIFICATION_WINDOW_EXPIRED",
            "2026-10-29T08:00:10Z")),
        list(domain("shop.example.com", "PENDING", "TXT_RECORD_NOT_FOUND",
            "2026-10-29T08:00:20Z")),
        list(domain("shop.example.com", "VERIFIED", null, "2026-10-29T08:00:40Z")));

    ProcessResult result = domains("verify", "shop.example.com", "--wait");

    result.assertSuccess();
    assertThat(result.stderr()).contains(msg.domainVerified("shop.example.com"));
  }

  @Test
  void shouldStopWaitingAtTheTimeout() throws Exception {
    ProcessResult result = domains("verify", "shop.example.com", "--wait", "--timeout", "1s");

    result.assertExitCode(1);
    assertThat(result.stderr())
        .contains(msg.domainVerificationTimedOut("shop.example.com", "1s"));
  }

  @Test
  void shouldStopWaitingWhenTheDomainIsRemoved() throws Exception {
    platform.queueLists(list(SHOP_PENDING), list(WWW_VERIFIED));

    ProcessResult result = domains("verify", "shop.example.com", "--wait");

    result.assertExitCode(1);
    assertThat(result.stderr())
        .contains(msg.domainRemovedWhileWaiting("shop.example.com", PROJECT));
  }

  @Test
  void shouldRejectInvalidTimeout() throws Exception {
    ProcessResult result = domains("verify", "shop.example.com", "--wait", "--timeout", "PT1H");

    result.assertExitCode(2);
    assertThat(result.stderr()).contains(msg.invalidDuration("PT1H"));
  }

  @Test
  void shouldShowWhichRecordsAreMissing() throws Exception {
    platform.queueChecks(StubDomainsServer.status("shop.example.com", "PENDING", "NOT_VERIFIED",
        "MISSING", "MISMATCH"));

    ProcessResult result = domains("status", "shop.example.com");

    result.assertExitCode(1);
    assertThat(result.stdout())
        .contains(msg.domainStatusHeader("shop.example.com", "PENDING", "NOT_VERIFIED",
            CHECKED_AT))
        .containsPattern("ownership\\s+TXT\\s+_streamx-verification\\.shop\\.example\\.com"
            + "\\s+MISSING")
        .containsPattern("certificate\\s+CNAME\\s+_acme-challenge\\.shop\\.example\\.com"
            + "\\s+MISMATCH")
        .contains(msg.domainRecordMissing("ownership", "TXT",
            "_streamx-verification.shop.example.com", "streamx-domain-verification=b3Jn"))
        .contains(msg.domainRecordMismatch("certificate", "_acme-challenge.shop.example.com",
            "old.acme.example.org", "0a1b2c3d.acme.example.net"));
    assertThat(result.stderr()).contains(msg.domainNotVerifiedYet("shop.example.com"));
  }

  @Test
  void shouldIgnoreStatusFieldsItDoesNotKnow() throws Exception {
    String status = StubDomainsServer.status("shop.example.com", "VERIFIED", "READY",
        "FOUND", "FOUND");
    platform.queueChecks(status.substring(0, status.lastIndexOf('}'))
        + ",\"gateways\":[{\"hostname\":\"" + EDGE_HOSTNAME + "\",\"status\":\"SERVED\"}]}");

    ProcessResult result = domains("status", "shop.example.com");

    result.assertSuccess();
    assertThat(result.stdout()).doesNotContain(EDGE_HOSTNAME);
    assertThat(result.stderr()).contains(msg.domainReadyForTraffic("shop.example.com"));
  }

  @Test
  void shouldExplainTheNextStepOfVerifiedDomain() throws Exception {
    platform.queueChecks(StubDomainsServer.status("shop.example.com", "VERIFIED", "NO_ROUTE",
        "FOUND", "FOUND"));

    ProcessResult result = domains("status", "shop.example.com");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.domainVerifiedNextStep("shop.example.com"));
  }

  @Test
  void shouldSucceedWhenTheDomainIsReady() throws Exception {
    platform.queueChecks(StubDomainsServer.status("shop.example.com", "VERIFIED", "READY",
        "FOUND", "FOUND"));

    ProcessResult result = domains("status", "shop.example.com");

    result.assertSuccess();
    assertThat(result.stderr()).contains(msg.domainReadyForTraffic("shop.example.com"));
    assertThat(platform.getRequests())
        .containsExactly(GET, "GET " + DOMAINS_PATH + "/shop.example.com/status");
  }

  @Test
  void shouldCheckStatusById() throws Exception {
    platform.queueChecks(StubDomainsServer.status("shop.example.com", "VERIFIED", "READY",
        "FOUND", "FOUND"));

    ProcessResult result = domains("status", "shop");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly(GET, "GET " + DOMAINS_PATH + "/shop.example.com/status");
    assertThat(result.stderr()).contains(msg.domainReadyForTraffic("shop.example.com"));
  }

  @Test
  void shouldWatchUntilTheDomainIsReady() throws Exception {
    platform.queueChecks(
        StubDomainsServer.status("shop.example.com", "PENDING", "NOT_VERIFIED",
            "MISSING", "MISSING"),
        StubDomainsServer.status("shop.example.com", "VERIFIED", "PENDING",
            "FOUND", "FOUND"),
        StubDomainsServer.status("shop.example.com", "VERIFIED", "READY",
            "FOUND", "FOUND"));

    ProcessResult result = domains("status", "shop.example.com", "--watch");

    result.assertSuccess();
    String pending = msg.domainStatusHeader("shop.example.com", "VERIFIED", "PENDING",
        CHECKED_AT);
    // Earlier rounds are progress on stderr; stdout holds the final result only.
    assertThat(result.stdout())
        .contains(msg.domainStatusHeader("shop.example.com", "VERIFIED", "READY", CHECKED_AT))
        .doesNotContain(pending);
    assertThat(result.stderr())
        .contains(msg.domainStatusHeader("shop.example.com", "PENDING", "NOT_VERIFIED",
            CHECKED_AT))
        .contains(pending)
        .contains(msg.domainReadyForTraffic("shop.example.com"));
  }

  @Test
  void shouldRejectStatusOfUnknownHostname() throws Exception {
    ProcessResult result = domains("status", "blog.example.com");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.domainNotConfigured("blog.example.com", PROJECT));
  }

  @Test
  void shouldKeepRawStatusValuesInJson() throws Exception {
    platform.queueChecks(StubDomainsServer.status("shop.example.com", "VERIFIED",
        "CERTIFICATE_PROBLEM", "FOUND", "LOOKUP_FAILED"));

    ProcessResult result = domains("status", "shop.example.com", "-o", "json");

    result.assertExitCode(1);
    assertThat(result.stdout())
        .contains("\"tls\" : \"CERTIFICATE_PROBLEM\"")
        .contains("\"purpose\" : \"CERTIFICATE\"")
        .contains("\"status\" : \"LOOKUP_FAILED\"");
    assertThat(result.stderr())
        .contains(msg.domainCertificateProblem("shop.example.com"));
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
