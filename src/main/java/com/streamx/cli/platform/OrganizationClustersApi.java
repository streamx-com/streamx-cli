package com.streamx.cli.platform;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

public class OrganizationClustersApi {

  private final PlatformApiClient client;

  public OrganizationClustersApi(PlatformApiClient client) {
    this.client = client;
  }

  public List<Cluster> list(String orgId) {
    JsonNode response = client.get(
        "/api/v1/organizations/" + PathSegments.encode(orgId) + "/clusters");

    List<Cluster> clusters = new ArrayList<>();
    for (JsonNode node : response.path("processing")) {
      clusters.add(Cluster.fromJson(node, "processing"));
    }
    for (JsonNode node : response.path("edge")) {
      clusters.add(Cluster.fromJson(node, "edge"));
    }
    return clusters;
  }
}
