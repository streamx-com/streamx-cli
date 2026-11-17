package com.streamx.cli.commands.project.domains;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.platform.DnsRecordView;
import com.streamx.cli.platform.ProjectDomainView;
import com.streamx.cli.platform.generated.model.DomainStatus.DnsChecksEnum;
import com.streamx.cli.platform.generated.model.DomainStatus.TlsCertificateEnum;
import com.streamx.cli.platform.generated.model.DomainStatusRecordsInner.StatusEnum;
import com.streamx.cli.platform.generated.model.RecheckDomainDns200Response.StateEnum;
import com.streamx.cli.platform.generated.model.RecheckDomainDns200ResponseDnsRecordsInner.TypeEnum;
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
        TypeEnum.TXT, "_streamx-verification.shop.example.com",
        "streamx-domain-verification=a\"b\\c")))
        .isEqualTo("_streamx-verification.shop.example.com. IN TXT "
            + "\"streamx-domain-verification=a\\\"b\\\\c\"");
    assertThat(DomainsSupport.formatDnsZoneRecord(new DnsRecordView(
        TypeEnum.CNAME, "_acme-challenge.shop.example.com", "0a1b.acme.example.net.")))
        .isEqualTo("_acme-challenge.shop.example.com. IN CNAME 0a1b.acme.example.net.");
  }

  @Test
  void reasonsAreReadableAndUnknownOnesStayRaw() {
    assertThat(DomainsSupport.describeDnsCheckReason("OWNERSHIP_RECORD_NOT_MATCHED"))
        .isEqualTo(msg.domainReasonOwnershipRecordNotMatched());
    assertThat(DomainsSupport.describeDnsCheckReason("TXT_RECORD_NOT_FOUND"))
        .isEqualTo(msg.domainReasonOwnershipRecordNotMatched());
    assertThat(DomainsSupport.describeDnsCheckReason("ACME_DELEGATION_RECORD_MISMATCH"))
        .isEqualTo(msg.domainReasonAcmeDelegationRecordMismatch());
    assertThat(DomainsSupport.describeDnsCheckReason("ORGANIZATION_NOT_RESOLVED"))
        .isEqualTo(msg.domainReasonOrganizationNotResolved());
    assertThat(DomainsSupport.describeDnsCheckReason("SOMETHING_NEW"))
        .isEqualTo("SOMETHING_NEW");
    assertThat(DomainsSupport.describeDnsCheckReason("DNS_CHECK_WINDOW_EXPIRED"))
        .isEqualTo(msg.domainReasonDnsCheckWindowExpired());
    assertThat(DomainsSupport.describeDnsCheckReason("VERIFICATION_WINDOW_EXPIRED"))
        .isEqualTo(msg.domainReasonDnsCheckWindowExpired());
    assertThat(DomainsSupport.describeDnsCheckReason(null)).isNull();
  }

  @Test
  void domainNamesAreNormalizedLikeTheApiDoes() {
    assertThat(DomainsSupport.normalizeDomain("  Shop.EXAMPLE.com "))
        .isEqualTo("shop.example.com");
  }

  @Test
  void statusLabelsAreReadable() {
    assertThat(DomainsSupport.describeVerificationState(StateEnum.PENDING))
        .isEqualTo("Checking DNS records");
    assertThat(DomainsSupport.describeDnsChecks(DnsChecksEnum.PASSED))
        .isEqualTo("DNS checks passed");
    assertThat(DomainsSupport.describeTlsCertificate(TlsCertificateEnum.TLS_CERTIFICATE_NOT_READY))
        .isEqualTo("Not ready");
    assertThat(DomainsSupport.describeTlsCertificate(TlsCertificateEnum.DOMAIN_NOT_IN_MESH_ROUTES))
        .isEqualTo("No mesh route uses this domain");
    assertThat(DomainsSupport.describeTlsCertificate(TlsCertificateEnum.TLS_CERTIFICATE_INVALID))
        .isEqualTo("Invalid certificate");
    assertThat(DomainsSupport.describeDnsRecord(StatusEnum.DNS_RECORD_VALUE_MISMATCH))
        .isEqualTo("Record mismatch");
    assertThat(DomainsSupport.describeDnsRecord(StatusEnum.DNS_LOOKUP_FAILED))
        .isEqualTo("DNS lookup failed");
    assertThat(DomainsSupport.describeDnsRecord(null)).isEqualTo("Unknown");
  }

  private static ProjectDomainView domain(String id, String hostname) {
    return new ProjectDomainView(id, hostname, StateEnum.PENDING, null, null, null, null, null,
        List.of());
  }
}
