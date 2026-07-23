package com.streamx.cli.platform;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  /**
   * The id is server-derived from the name and returned in the 201 body; it is never computed
   * client-side.
   */
  public Project create(String orgId, String name, String description) {
    return Project.fromJson(client.postJson(projectsPath(orgId), request(name, description)));
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
