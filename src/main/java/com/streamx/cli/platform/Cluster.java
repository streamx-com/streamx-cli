package com.streamx.cli.platform;

import com.streamx.cli.platform.generated.model.ClusterLocation;
import com.streamx.cli.platform.generated.model.ClustersProcessingInner;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Cluster(
    String id,
    String type,
    String name,
    boolean enabled,
    Double latitude,
    Double longitude
) {

  public static Cluster from(ClustersProcessingInner node, String type) {
    ClusterLocation location = node.getLocation();
    return new Cluster(
        node.getId(),
        type,
        node.getName(),
        Boolean.TRUE.equals(node.getEnabled()),
        location == null ? null : location.getLatitude(),
        location == null ? null : location.getLongitude());
  }
}
