package com.streamx.cli.commands.project.create;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.ClusterIdCompletionCandidates;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.Project;
import com.streamx.cli.platform.ProjectsApi;
import com.streamx.cli.platform.ProjectsApi.RepositorySettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "create",
    header = "Create a project",
    description = "Optionally connects a Git repository and enables clusters in the same call. "
        + "The endpoint is transactional: if any part fails, nothing is created."
)
public class CreateCommand extends AbstractCommand<Project> {

  @CommandLine.Option(
      names = "--org",
      paramLabel = "<orgId>",
      description = "Organization ID (defaults to the current organization)",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @CommandLine.Parameters(
      index = "0",
      description = "Project name"
  )
  public String name;

  @CommandLine.Option(
      names = {"-d", "--description"},
      description = "Project description"
  )
  public String description;

  @CommandLine.ArgGroup(exclusive = false)
  public RepositoryOptions repository;

  public static class RepositoryOptions {

    @CommandLine.Option(
        names = "--repository-uri",
        required = true,
        paramLabel = "<uri>",
        description = "Git repository URI to connect to the project"
    )
    public String uri;

    @CommandLine.Option(
        names = "--repository-branch",
        required = true,
        paramLabel = "<branch>",
        description = "Git branch the platform deploys from"
    )
    public String branch;

    @CommandLine.Option(
        names = "--ssh-private-key",
        paramLabel = "<file>",
        description = "SSH private key file for private repositories (sent base64-encoded)"
    )
    public Path sshPrivateKey;
  }

  @CommandLine.Option(
      names = "--cluster",
      paramLabel = "<clusterId>",
      description = "Cluster to enable for the project (repeatable)",
      completionCandidates = ClusterIdCompletionCandidates.class
  )
  public List<String> clusters;

  @Override
  public String getTextOutput(CommandResult<Project> result) {
    Project project = result.getData();
    return msg.projectCreated(name, project.id());
  }

  @Override
  public CommandResult<Project> runCommand() {
    orgId = PlatformContext.requireOrg(orgId);
    RepositorySettings repositorySettings = repository == null ? null
        : new RepositorySettings(repository.uri, repository.branch, readKeyBase64());
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      return new CommandResult<>(new ProjectsApi(client)
          .create(orgId, name, description, repositorySettings, clusters));
    }
  }

  private String readKeyBase64() {
    if (repository.sshPrivateKey == null) {
      return null;
    }
    try {
      return Base64.getEncoder().encodeToString(Files.readAllBytes(repository.sshPrivateKey));
    } catch (IOException e) {
      throw new CliException(
          msg.projectSshKeyFileUnreadable(repository.sshPrivateKey.toString(), e.getMessage()), e);
    }
  }
}
