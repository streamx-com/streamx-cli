package com.streamx.cli.platform;

import com.streamx.cli.platform.generated.api.PendingChangesResourceApi;
import com.streamx.cli.platform.generated.api.ProjectsResourceApi;
import com.streamx.cli.platform.generated.api.StatusResourceApi;
import com.streamx.cli.platform.generated.model.CreateProjectRequest;
import com.streamx.cli.platform.generated.model.CreateProjectRequestRepository;
import com.streamx.cli.platform.generated.model.PendingChange;
import com.streamx.cli.platform.generated.model.Project;
import com.streamx.cli.platform.generated.model.ProjectRequest;
import com.streamx.cli.platform.generated.model.ProjectStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ProjectsApi {

  private final PlatformClients clients;
  private final ProjectsResourceApi api;

  public ProjectsApi(PlatformClients clients) {
    this.clients = clients;
    this.api = clients.api(ProjectsResourceApi.class);
  }

  public record RepositorySettings(String uri, String branch, String sshPrivateKeyBase64) {
  }

  public List<Project> list(String orgId) {
    return clients.callList(() -> api.listProjects(orgId, null, null), Project.class).stream()
        .sorted(Comparator.comparing(Project::getId, Comparator.nullsLast(String::compareTo)))
        .toList();
  }

  public Project get(String orgId, String projectId) {
    return clients.call(() -> api.getProject(orgId, projectId, null, null), Project.class);
  }

  public ProjectView getDetailed(String orgId, String projectId) {
    return detailed(orgId, get(orgId, projectId));
  }

  public ProjectView detailed(String orgId, Project project) {
    List<String> clusters = new OrganizationClustersApi(clients)
        .listForProject(orgId, project.getId()).stream()
        .filter(Cluster::enabled)
        .map(Cluster::id)
        .filter(Objects::nonNull)
        .toList();
    RepositoryView repository = null;
    try {
      repository = RepositoryView.from(
          new ProjectRepositoryApi(clients).get(orgId, project.getId()));
    } catch (PlatformClients.NotFoundException notConnected) {
      repository = null;
    }
    return ProjectView.of(project, clusters, repository);
  }

  public Project create(String orgId, String name, String description,
      RepositorySettings repository, List<String> clusters) {
    CreateProjectRequest request = new CreateProjectRequest().name(name).description(description);
    if (repository != null) {
      request.repository(new CreateProjectRequestRepository()
          .uri(repository.uri())
          .branch(repository.branch())
          .sshPrivateKeyBase64(repository.sshPrivateKeyBase64()));
    }
    if (clusters != null && !clusters.isEmpty()) {
      request.clusters(clusters);
    }
    return clients.call(() -> api.createProject(orgId, request, null, null), Project.class);
  }

  public Project update(String orgId, String projectId, String name, String description) {
    ProjectRequest request = new ProjectRequest().name(name).description(description);
    return clients.call(
        () -> api.updateProject(orgId, projectId, request, null, null), Project.class);
  }

  public void delete(String orgId, String projectId) {
    clients.call(() -> api.deleteProject(orgId, projectId, null, null));
  }

  public ProjectStatus status(String orgId, String projectId) {
    StatusResourceApi statusApi = clients.api(StatusResourceApi.class);
    return clients.call(() -> statusApi.status(orgId, projectId, null, null), ProjectStatus.class);
  }

  public List<PendingChange> pendingChanges(String orgId, String projectId) {
    PendingChangesResourceApi changesApi = clients.api(PendingChangesResourceApi.class);
    return clients.callList(
        () -> changesApi.getPendingChanges(orgId, projectId, null, null), PendingChange.class);
  }
}
