package com.streamx.cli.platform;

import com.streamx.cli.platform.generated.api.OrganizationsResourceApi;
import com.streamx.cli.platform.generated.model.Name;
import com.streamx.cli.platform.generated.model.Organization;
import java.util.Comparator;
import java.util.List;

public class OrganizationsApi {

  private final PlatformClients clients;
  private final OrganizationsResourceApi api;

  public OrganizationsApi(PlatformClients clients) {
    this.clients = clients;
    this.api = clients.api(OrganizationsResourceApi.class);
  }

  public List<Organization> list() {
    return clients.callList(() -> api.listOrganizations(null, null), Organization.class).stream()
        .sorted(Comparator.comparing(Organization::getId, Comparator.nullsLast(String::compareTo)))
        .toList();
  }

  public Organization get(String orgId) {
    return clients.call(() -> api.getOrganization(orgId, null, null), Organization.class);
  }

  public void create(String name) {
    clients.call(() -> api.createOrganization(new Name().name(name), null, null));
  }

  public void delete(String orgId) {
    clients.call(() -> api.deleteOrganization(orgId, null, null));
  }
}
