package com.streamx.cli.platform;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class OrganizationsApi {
  private static final String ORGANIZATIONS_PATH = "/api/v1/organizations";

  private final PlatformApiClient client;

  public OrganizationsApi(PlatformApiClient client) {
    this.client = client;
  }

  public List<Organization> list() {
    List<Organization> organizations = new ArrayList<>();
    for (JsonNode node : client.get(ORGANIZATIONS_PATH)) {
      organizations.add(Organization.fromJson(node));
    }
    organizations.sort(
        Comparator.comparing(Organization::id, Comparator.nullsLast(String::compareTo)));
    return organizations;
  }

  public Organization get(String orgId) {
    return Organization.fromJson(client.get(organizationPath(orgId)));
  }

  public void create(String name) {
    client.postJson(ORGANIZATIONS_PATH, Map.of("name", name));
  }

  public void delete(String orgId) {
    client.delete(organizationPath(orgId));
  }

  private static String organizationPath(String orgId) {
    return ORGANIZATIONS_PATH + "/" + PathSegments.encode(orgId);
  }
}
