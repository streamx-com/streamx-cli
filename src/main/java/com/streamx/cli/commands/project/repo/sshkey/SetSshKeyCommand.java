package com.streamx.cli.commands.project.repo.sshkey;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.commands.project.repo.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectRepositoryApi;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import picocli.CommandLine;

@CommandLine.Command(
    name = "set",
    header = "Set the SSH private key used to access the repository"
)
public class SetSshKeyCommand extends AbstractSilentCommand {

  @CommandLine.Mixin
  ProjectScopedOptions scope;

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "<file>",
      description = "SSH private key file (sent base64-encoded)"
  )
  public Path keyFile;

  @Override
  public CommandResult<Void> runCommand() {
    String keyBase64 = readKeyBase64();
    PlatformContext.OrgProject context =
        PlatformContext.orgAndProject(scope.orgId, scope.projectId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      ProjectRepositoryApi api = new ProjectRepositoryApi(client);
      boolean create = !api.sshKeyExists(context.org(), context.project());
      api.setSshKey(context.org(), context.project(), keyBase64, create);
    }
    System.out.println(msg.projectSshKeySet(context.project()));
    return new CommandResult<>(null);
  }

  private String readKeyBase64() {
    try {
      return Base64.getEncoder().encodeToString(Files.readAllBytes(keyFile));
    } catch (IOException e) {
      throw new CliException(
          msg.projectSshKeyFileUnreadable(keyFile.toString(), e.getMessage()), e);
    }
  }
}
