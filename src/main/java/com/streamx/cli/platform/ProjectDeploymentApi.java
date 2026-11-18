package com.streamx.cli.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.platform.generated.api.CloudConfigResourceApi;
import com.streamx.cli.platform.generated.model.CloudConfigDiffPathRequest;

public class ProjectDeploymentApi {

  private final PlatformClients clients;
  private final CloudConfigResourceApi api;

  public ProjectDeploymentApi(PlatformClients clients) {
    this.clients = clients;
    this.api = clients.api(CloudConfigResourceApi.class);
  }

  public JsonNode live(String orgId, String projectId) {
    return clients.call(() -> api.getLive(orgId, projectId, null, null), JsonNode.class);
  }

  public JsonNode desired(String orgId, String projectId) {
    return clients.call(() -> api.getDesired(orgId, projectId, null, null), JsonNode.class);
  }

  public JsonNode diff(String orgId, String projectId, String path) {
    if (path == null) {
      return clients.call(() -> api.getDiff(orgId, projectId, null, null), JsonNode.class);
    }
    CloudConfigDiffPathRequest request = new CloudConfigDiffPathRequest().path(path);
    return clients.call(
        () -> api.getDiffForPath(orgId, projectId, request, null, null), JsonNode.class);
  }

  public void discard(String orgId, String projectId) {
    clients.call(() -> api.discardLocalChanges(orgId, projectId, null, null));
  }

  public JsonNode push(String orgId, String projectId) {
    return clients.call(() -> api.push(orgId, projectId, null, null), JsonNode.class);
  }
}
