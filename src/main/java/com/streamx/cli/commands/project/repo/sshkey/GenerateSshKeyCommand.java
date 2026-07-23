package com.streamx.cli.commands.project.repo.sshkey;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.PlatformApiClient;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectRepositoryApi;
import com.streamx.cli.platform.SshKeyPair;
import picocli.CommandLine;

@CommandLine.Command(
    name = "generate",
    header = "Generate a new SSH key pair server-side",
    description = "The pair is only returned, not stored: save the private key and pass it to "
        + "'ssh-key set' or 'project create --ssh-private-key'; add the public key to the Git "
        + "hosting's deploy keys."
)
public class GenerateSshKeyCommand extends AbstractCommand<SshKeyPair> {

  @CommandLine.Option(
      names = "--org",
      paramLabel = "<orgId>",
      description = "Organization ID (defaults to the current organization)",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @Override
  public String getTextOutput(CommandResult<SshKeyPair> result) {
    SshKeyPair pair = result.getData();
    return pair.privateKey() + "\n" + pair.publicKey();
  }

  @Override
  public CommandResult<SshKeyPair> runCommand() {
    orgId = PlatformContext.requireOrg(orgId);
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      return new CommandResult<>(new ProjectRepositoryApi(client).generateKeyPair(orgId));
    }
  }
}
