package com.streamx.cli.commands.project.domains;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.platform.DnsRecordView;
import com.streamx.cli.platform.ProjectDomainView;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainsSupportTest {

  @Test
  void domainsAreFoundByIdOrDomain() {
    ProjectDomainView shop = domain("shop", "shop.example.com");
    ProjectDomainView www = domain("www", "www.example.com");
    List<ProjectDomainView> domains = List.of(shop, www);

    assertThat(DomainsSupport.findDomain(domains, "www")).contains(www);
    assertThat(DomainsSupport.findDomain(domains, "shop.example.com")).contains(shop);
    assertThat(DomainsSupport.findDomain(domains, "blog")).isEmpty();
  }

  @Test
  void zoneLinesUseAbsoluteNamesAndQuoteTxtValues() {
    assertThat(DomainsSupport.formatDnsZoneRecord(new DnsRecordView(
        "TXT", "_streamx-verification.shop.example.com", "streamx-domain-verification=a\"b\\c")))
        .isEqualTo("_streamx-verification.shop.example.com. IN TXT "
            + "\"streamx-domain-verification=a\\\"b\\\\c\"");
    assertThat(DomainsSupport.formatDnsZoneRecord(new DnsRecordView(
        "CNAME", "_acme-challenge.shop.example.com", "0a1b.acme.example.net.")))
        .isEqualTo("_acme-challenge.shop.example.com. IN CNAME 0a1b.acme.example.net.");
  }

  @Test
  void reasonsAreReadableAndUnknownOnesStayRaw() {
    assertThat(DomainsSupport.describeVerificationReason("CNAME_TARGET_MISMATCH"))
        .isEqualTo(msg.domainReasonCnameTargetMismatch());
    assertThat(DomainsSupport.describeVerificationReason("ORGANIZATION_NOT_RESOLVED"))
        .isEqualTo(msg.domainReasonOrganizationNotResolved());
    assertThat(DomainsSupport.describeVerificationReason("SOMETHING_NEW"))
        .isEqualTo("SOMETHING_NEW");
    assertThat(DomainsSupport.describeVerificationReason(null)).isNull();
  }

  @Test
  void domainNamesAreNormalizedLikeTheApiDoes() {
    assertThat(DomainsSupport.normalizeDomain("  Shop.EXAMPLE.com "))
        .isEqualTo("shop.example.com");
  }

  @Test
  void statusLabelsAreReadableAndUnknownValuesArePreserved() {
    assertThat(DomainsSupport.describeStatus("PENDING")).isEqualTo("Verifying");
    assertThat(DomainsSupport.describeHttpsStatus("PENDING")).isEqualTo("Setting up HTTPS");
    assertThat(DomainsSupport.describeStatus("NO_ROUTE")).isEqualTo("No route configured");
    assertThat(DomainsSupport.describeStatus("CERTIFICATE_PROBLEM"))
        .isEqualTo("Certificate error");
    assertThat(DomainsSupport.describeStatus("MISMATCH")).isEqualTo("Record value doesn’t match");
    assertThat(DomainsSupport.describeStatus("LOOKUP_FAILED")).isEqualTo("DNS lookup failed");
    assertThat(DomainsSupport.describeStatus("SOMETHING_NEW")).isEqualTo("SOMETHING_NEW");
    assertThat(DomainsSupport.describeStatus(null)).isEqualTo("Unknown");
  }

  private static ProjectDomainView domain(String id, String hostname) {
    return new ProjectDomainView(id, hostname, "PENDING", null, null, null, null, null, List.of());
  }
}
