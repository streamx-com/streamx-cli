package com.streamx.cli.platform;

import com.streamx.cli.platform.generated.api.ClusterApi;
import com.streamx.cli.platform.generated.model.Clusters;
import com.streamx.cli.platform.generated.model.ClustersProcessingInner;
import java.util.ArrayList;
import java.util.List;

public class OrganizationClustersApi {

  private final PlatformClients clients;
  private final ClusterApi api;

  public OrganizationClustersApi(PlatformClients clients) {
    this.clients = clients;
    this.api = clients.api(ClusterApi.class);
  }

  public List<Cluster> list(String orgId) {
    return flatten(
        clients.call(() -> api.listOrganizationClusters(orgId, null, null), Clusters.class));
  }

  private static List<Cluster> flatten(Clusters clusters) {
    List<Cluster> result = new ArrayList<>();
    if (clusters == null) {
      return result;
    }
    for (ClustersProcessingInner node : orEmpty(clusters.getProcessing())) {
      result.add(Cluster.from(node, "processing"));
    }
    for (ClustersProcessingInner node : orEmpty(clusters.getEdge())) {
      result.add(Cluster.from(node, "edge"));
    }
    return result;
  }

  private static List<ClustersProcessingInner> orEmpty(List<ClustersProcessingInner> list) {
    return list == null ? List.of() : list;
  }
}
