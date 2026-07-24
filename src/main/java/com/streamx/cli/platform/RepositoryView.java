package com.streamx.cli.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.streamx.cli.platform.generated.model.ProjectRepository;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RepositoryView(String uri, String branch, boolean sshKeyProvided) {

  public static RepositoryView from(ProjectRepository repository) {
    return new RepositoryView(
        repository.getUri(), repository.getBranch(),
        Boolean.TRUE.equals(repository.getSshKeyProvided()));
  }
}
