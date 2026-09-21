package com.streamx.cli.platform;

import com.streamx.cli.platform.generated.api.ProjectRepositoryResourceApi;
import com.streamx.cli.platform.generated.model.ProjectRepository;

public class ProjectRepositoryApi {

  private final PlatformClients clients;
  private final ProjectRepositoryResourceApi api;

  public ProjectRepositoryApi(PlatformClients clients) {
    this.clients = clients;
    this.api = clients.api(ProjectRepositoryResourceApi.class);
  }

  public ProjectRepository get(String orgId, String projectId) {
    return clients.call(
        () -> api.getProjectRepository(orgId, projectId, null, null), ProjectRepository.class);
  }
}
