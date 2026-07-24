package com.streamx.cli.platform;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** {@code /api/v1/organizations/{orgId}/projects} — mirrors the Projects Resource in the spec. */
public class ProjectsApi {

  private final PlatformApiClient client;

  public ProjectsApi(PlatformApiClient client) {
    this.client = client;
  }

  public List<Project> list(String orgId) {
    List<Project> projects = new ArrayList<>();
    for (JsonNode node : client.get(projectsPath(orgId))) {
      projects.add(Project.fromJson(node));
    }
    projects.sort(Comparator.comparing(Project::id, Comparator.nullsLast(String::compareTo)));
    return projects;
  }

  public Project get(String orgId, String projectId) {
    return Project.fromJson(client.get(projectPath(orgId, projectId)));
  }

  public ProjectView getDetailed(String orgId, String projectId) {
    return detailed(orgId, get(orgId, projectId));
  }

  public ProjectView detailed(String orgId, Project project) {
    List<String> clusters = new OrganizationClustersApi(client)
        .listForProject(orgId, project.id()).stream()
        .filter(Cluster::enabled)
        .map(Cluster::id)
        .filter(Objects::nonNull)
        .toList();
    RepositoryView repository = null;
    try {
      repository = RepositoryView.from(new ProjectRepositoryApi(client).get(orgId, project.id()));
    } catch (PlatformApiClient.NotFoundException notConnected) {
      repository = null;
    }
    return ProjectView.of(project, clusters, repository);
  }

  /** Optional repository settings of {@link #create}, mirroring the spec's RepositorySettings. */
  public record RepositorySettings(String uri, String branch, String sshPrivateKeyBase64) {
  }

  /**
   * The id is server-derived from the name and returned in the 201 body; it is never computed
   * client-side. Repository and clusters are optional; the endpoint is transactional and rolls
   * everything back if any part fails.
   */
  public Project create(String orgId, String name, String description,
      RepositorySettings repository, List<String> clusters) {
    Map<String, Object> body = new HashMap<>(request(name, description));
    if (repository != null) {
      Map<String, String> repo = new HashMap<>();
      repo.put("uri", repository.uri());
      repo.put("branch", repository.branch());
      if (repository.sshPrivateKeyBase64() != null) {
        repo.put("sshPrivateKeyBase64", repository.sshPrivateKeyBase64());
      }
      body.put("repository", repo);
    }
    if (clusters != null && !clusters.isEmpty()) {
      body.put("clusters", clusters);
    }
    return Project.fromJson(client.postJson(projectsPath(orgId), body));
  }

  public Project update(String orgId, String projectId, String name, String description) {
    return Project.fromJson(
        client.patchJson(projectPath(orgId, projectId), request(name, description)));
  }

  public void delete(String orgId, String projectId) {
    client.delete(projectPath(orgId, projectId));
  }

  public ProjectStatus status(String orgId, String projectId) {
    return ProjectStatus.fromJson(client.get(projectPath(orgId, projectId) + "/status"));
  }

  public List<PendingChange> pendingChanges(String orgId, String projectId) {
    List<PendingChange> changes = new ArrayList<>();
    for (JsonNode node : client.get(projectPath(orgId, projectId) + "/changes/pending")) {
      changes.add(PendingChange.fromJson(node));
    }
    return changes;
  }

  /** {@code name} is required by ProjectRequest; description is only sent when present. */
  private static Map<String, String> request(String name, String description) {
    Map<String, String> body = new HashMap<>();
    body.put("name", name);
    if (description != null) {
      body.put("description", description);
    }
    return body;
  }

  private static String projectsPath(String orgId) {
    return "/api/v1/organizations/" + PathSegments.encode(orgId) + "/projects";
  }

  private static String projectPath(String orgId, String projectId) {
    return projectsPath(orgId) + "/" + PathSegments.encode(projectId);
  }
}
