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
    return parse(client.get(
        "/api/v1/organizations/" + PathSegments.encode(orgId) + "/clusters"));
  }

  public List<Cluster> listForProject(String orgId, String projectId) {
    return parse(client.get(projectClustersPath(orgId, projectId)));
  }

  public void setForProject(String orgId, String projectId, List<String> clusterIds) {
    client.patchJson(projectClustersPath(orgId, projectId), clusterIds);
  }

  private static String projectClustersPath(String orgId, String projectId) {
    return "/api/v1/organizations/" + PathSegments.encode(orgId)
        + "/projects/" + PathSegments.encode(projectId) + "/clusters";
  }

  private static List<Cluster> parse(JsonNode response) {
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
