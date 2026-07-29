package com.streamx.cli.commands.project.repo;

import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.ProjectIdCompletionCandidates;
import picocli.CommandLine;

/** The org/project selectors shared by every {@code project repo} subcommand. */
public class ProjectScopedOptions {

  @CommandLine.Option(
      names = "--org",
      paramLabel = "<orgId>",
      description = "Organization ID (defaults to the current organization)",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @CommandLine.Option(
      names = "--project",
      paramLabel = "<projectId>",
      description = "Project ID (defaults to the current project)",
      completionCandidates = ProjectIdCompletionCandidates.class
  )
  public String projectId;
}
