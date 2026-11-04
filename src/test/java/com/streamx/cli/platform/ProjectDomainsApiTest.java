package com.streamx.cli.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProjectDomainsApiTest {

  private static final Map<String, String> DOMAINS = new LinkedHashMap<>(Map.of(
      "shop", "shop.example.com", "www", "www.example.com", "blog", "blog.example.com"));

  @Test
  void fieldErrorsNameTheHostnameOfTheirId() {
    String rejected = ProjectDomainsApi.rejected(List.of(
        new PlatformClients.Violation("domains[blog]", "Duplicate domain"),
        new PlatformClients.Violation(
            "updateProjectDomains.domainsRequest.domains[shop].<map value>",
            "Invalid domain name"),
        new PlatformClients.Violation(
            "updateProjectDomains.domainsRequest.domains<K>[www].<map key>", "must not be null")),
        DOMAINS);

    assertThat(rejected).isEqualTo(
        "  blog.example.com: Duplicate domain" + System.lineSeparator()
            + "  shop.example.com: Invalid domain name" + System.lineSeparator()
            + "  www.example.com: must not be null");
  }

  @Test
  void fieldErrorsWithoutKnownIdKeepTheirField() {
    String rejected = ProjectDomainsApi.rejected(List.of(
        new PlatformClients.Violation("domains[news]", "Blank domain"),
        new PlatformClients.Violation("domains", "must not be null")), DOMAINS);

    assertThat(rejected).isEqualTo(
        "  domains[news]: Blank domain" + System.lineSeparator() + "  domains: must not be null");
  }

  @Test
  void blankHostnamesKeepTheirField() {
    String rejected = ProjectDomainsApi.rejected(
        List.of(new PlatformClients.Violation("domains[empty]", "Blank domain")),
        Map.of("shop", "shop.example.com", "empty", ""));

    assertThat(rejected).isEqualTo("  domains[empty]: Blank domain");
  }
}
