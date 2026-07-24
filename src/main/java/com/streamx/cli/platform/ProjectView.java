package com.streamx.cli.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectView(
    String id,
    String name,
    String description,
    String state,
    List<String> clusters,
    RepositoryView repository
) {

  public static ProjectView basic(Project project) {
    return new ProjectView(
        project.id(), project.name(), project.description(), project.state(), null, null);
  }

  public static ProjectView of(
      Project project, List<String> clusters, RepositoryView repository) {
    return new ProjectView(
        project.id(), project.name(), project.description(), project.state(),
        clusters, repository);
  }
}
