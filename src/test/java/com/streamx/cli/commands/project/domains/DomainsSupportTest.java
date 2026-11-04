package com.streamx.cli.commands.project.domains;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.platform.DnsRecordView;
import com.streamx.cli.platform.ProjectDomainView;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainsSupportTest {

  @Test
  void domainsAreFoundByIdOrHostname() {
    ProjectDomainView shop = domain("shop", "shop.example.com");
    ProjectDomainView www = domain("www", "www.example.com");
    List<ProjectDomainView> domains = List.of(shop, www);

    assertThat(DomainsSupport.find(domains, "www")).contains(www);
    assertThat(DomainsSupport.find(domains, "shop.example.com")).contains(shop);
    assertThat(DomainsSupport.find(domains, "blog")).isEmpty();
  }

  @Test
  void zoneLinesUseAbsoluteNamesAndQuoteTxtValues() {
    assertThat(DomainsSupport.zoneLine(new DnsRecordView(
        "TXT", "_streamx-verification.shop.example.com", "streamx-domain-verification=a\"b\\c")))
        .isEqualTo("_streamx-verification.shop.example.com. IN TXT "
            + "\"streamx-domain-verification=a\\\"b\\\\c\"");
    assertThat(DomainsSupport.zoneLine(new DnsRecordView(
        "CNAME", "_acme-challenge.shop.example.com", "0a1b.acme.example.net.")))
        .isEqualTo("_acme-challenge.shop.example.com. IN CNAME 0a1b.acme.example.net.");
  }

  @Test
  void reasonsAreReadableAndUnknownOnesStayRaw() {
    assertThat(DomainsSupport.reasonText("CNAME_TARGET_MISMATCH"))
        .isEqualTo(msg.domainReasonCnameTargetMismatch());
    assertThat(DomainsSupport.reasonText("ORGANIZATION_NOT_RESOLVED"))
        .isEqualTo(msg.domainReasonOrganizationNotResolved());
    assertThat(DomainsSupport.reasonText("SOMETHING_NEW")).isEqualTo("SOMETHING_NEW");
    assertThat(DomainsSupport.reasonText(null)).isNull();
  }

  @Test
  void hostnamesAreNormalizedLikeTheApiDoes() {
    assertThat(DomainsSupport.normalize("  Shop.EXAMPLE.com ")).isEqualTo("shop.example.com");
  }

  @Test
  void idsAreTheFirstLabelOfTheHostname() {
    assertThat(DomainsSupport.deriveId(" Shop.Example.com ", List.of())).isEqualTo("shop");
    assertThat(DomainsSupport.deriveId("example.com", List.of())).isEqualTo("example");
  }

  @Test
  void takenIdsAreNumbered() {
    assertThat(DomainsSupport.deriveId("www.example.org", List.of("www", "www-2")))
        .isEqualTo("www-3");
    assertThat(DomainsSupport.deriveId(" Shop.Example.com ", List.of("shop", "shop-2")))
        .isEqualTo("shop-3");
  }

  @Test
  void numberedIdsStayWithinTheLengthLimit() {
    String label = "a".repeat(63);

    String id = DomainsSupport.deriveId(label + ".example.com", List.of(label));

    assertThat(id).hasSize(63).endsWith("-2");
  }

  private static ProjectDomainView domain(String id, String hostname) {
    return new ProjectDomainView(id, hostname, "PENDING", null, null, null, null, null, List.of());
  }
}
