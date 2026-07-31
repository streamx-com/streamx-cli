package com.streamx.cli.platform;

import com.streamx.cli.platform.generated.api.ProjectRepositoryResourceApi;
import com.streamx.cli.platform.generated.api.ProjectRepositorySshKeyResourceApi;
import com.streamx.cli.platform.generated.api.ProjectsResourceApi;
import com.streamx.cli.platform.generated.model.CreateProjectRepositoryRequest;
import com.streamx.cli.platform.generated.model.PrivateKeyRequest;
import com.streamx.cli.platform.generated.model.PrivatePublicKeyPair;
import com.streamx.cli.platform.generated.model.ProjectRepository;
import com.streamx.cli.platform.generated.model.PublicKey;

public class ProjectRepositoryApi {

  private final PlatformClients clients;
  private final ProjectRepositoryResourceApi api;
  private final ProjectRepositorySshKeyResourceApi sshKeyApi;

  public ProjectRepositoryApi(PlatformClients clients) {
    this.clients = clients;
    this.api = clients.api(ProjectRepositoryResourceApi.class);
    this.sshKeyApi = clients.api(ProjectRepositorySshKeyResourceApi.class);
  }

  public ProjectRepository get(String orgId, String projectId) {
    return clients.call(
        () -> api.getProjectRepository(orgId, projectId, null, null), ProjectRepository.class);
  }

  public void connect(String orgId, String projectId, String uri, String branch) {
    clients.call(() -> api.createProjectRepository(orgId, projectId,
        new CreateProjectRepositoryRequest().uri(uri).branch(branch), null, null));
  }

  public void update(String orgId, String projectId, String uri, String branch) {
    clients.call(() -> api.updateProjectRepository(orgId, projectId,
        new CreateProjectRepositoryRequest().uri(uri).branch(branch), null, null));
  }

  public void disconnect(String orgId, String projectId) {
    clients.call(() -> api.deleteProjectRepository(orgId, projectId, null, null));
  }

  public boolean sshKeyExists(String orgId, String projectId) {
    return Boolean.TRUE.equals(clients.call(
        () -> sshKeyApi.hasProjectRepositorySshKey(orgId, projectId, null, null), Boolean.class));
  }

  public void setSshKey(String orgId, String projectId, String privateKeyBase64, boolean create) {
    PrivateKeyRequest body = new PrivateKeyRequest().privateKeyBase64(privateKeyBase64);
    if (create) {
      clients.call(
          () -> sshKeyApi.createProjectRepositorySshKey(orgId, projectId, body, null, null));
    } else {
      clients.call(
          () -> sshKeyApi.updateProjectRepositorySshKey(orgId, projectId, body, null, null));
    }
  }

  public void removeSshKey(String orgId, String projectId) {
    clients.call(() -> sshKeyApi.deleteProjectRepositorySshKey(orgId, projectId, null, null));
  }

  public String publicKey(String orgId, String projectId) {
    PublicKey key = clients.call(
        () -> sshKeyApi.getSshPublicKey(orgId, projectId, null, null), PublicKey.class);
    return key == null ? null : key.getPublicKey();
  }

  public PrivatePublicKeyPair generateKeyPair(String orgId) {
    ProjectsResourceApi projectsApi = clients.api(ProjectsResourceApi.class);
    return clients.call(
        () -> projectsApi.generateSshKeyPair(orgId, null, null), PrivatePublicKeyPair.class);
  }
}
