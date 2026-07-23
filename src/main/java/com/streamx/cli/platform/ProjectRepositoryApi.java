package com.streamx.cli.platform;

import java.util.Map;

/**
 * {@code /api/v1/organizations/{orgId}/projects/{projectId}/repository} and its ssh-key
 * sub-resource - mirrors the Project Repository Resources in the spec.
 */
public class ProjectRepositoryApi {

  private final PlatformApiClient client;

  public ProjectRepositoryApi(PlatformApiClient client) {
    this.client = client;
  }

  public ProjectRepository get(String orgId, String projectId) {
    return ProjectRepository.fromJson(client.get(repositoryPath(orgId, projectId)));
  }

  public void connect(String orgId, String projectId, String uri, String branch) {
    client.postJson(repositoryPath(orgId, projectId), Map.of("uri", uri, "branch", branch));
  }

  public void update(String orgId, String projectId, String uri, String branch) {
    client.patchJson(repositoryPath(orgId, projectId), Map.of("uri", uri, "branch", branch));
  }

  public void disconnect(String orgId, String projectId) {
    client.delete(repositoryPath(orgId, projectId));
  }

  public boolean sshKeyExists(String orgId, String projectId) {
    return client.get(sshKeyPath(orgId, projectId) + "/exists").asBoolean(false);
  }

  /** POST creates the first key; PATCH replaces an existing one. */
  public void setSshKey(String orgId, String projectId, String privateKeyBase64, boolean create) {
    Map<String, String> body = Map.of("privateKeyBase64", privateKeyBase64);
    if (create) {
      client.postJson(sshKeyPath(orgId, projectId), body);
    } else {
      client.patchJson(sshKeyPath(orgId, projectId), body);
    }
  }

  public void removeSshKey(String orgId, String projectId) {
    client.delete(sshKeyPath(orgId, projectId));
  }

  public String publicKey(String orgId, String projectId) {
    return client.get(sshKeyPath(orgId, projectId) + "/public-key").path("publicKey").asText(null);
  }

  /** Org-level helper: mints a key pair server-side without attaching it to any project. */
  public SshKeyPair generateKeyPair(String orgId) {
    return SshKeyPair.fromJson(client.postJson(
        "/api/v1/organizations/" + PathSegments.encode(orgId)
            + "/projects/repository/ssh-key/generate-key-pair",
        Map.of()));
  }

  private static String repositoryPath(String orgId, String projectId) {
    return "/api/v1/organizations/" + PathSegments.encode(orgId)
        + "/projects/" + PathSegments.encode(projectId) + "/repository";
  }

  private static String sshKeyPath(String orgId, String projectId) {
    return repositoryPath(orgId, projectId) + "/ssh-key";
  }
}
