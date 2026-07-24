package com.streamx.cli.commands.project.repo.sshkey;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectRepositoryApi;
import com.streamx.cli.platform.generated.model.PrivatePublicKeyPair;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import picocli.CommandLine;

@CommandLine.Command(
    name = "generate",
    header = "Generate a new SSH key pair server-side and save it to files",
    description = "The private key is written to <file> (mode 600) and the public key to "
        + "<file>.pub; the keys are not printed and not stored on the platform. Pass the "
        + "private key to 'ssh-key set' or 'project create --ssh-private-key'; add the "
        + "public key to the Git hosting's deploy keys."
)
public class GenerateSshKeyCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "<file>",
      description = "Where to save the private key; the public key goes to <file>.pub"
  )
  public Path keyFile;

  @CommandLine.Option(
      names = "--org",
      paramLabel = "<orgId>",
      description = "Organization ID (defaults to the current organization)",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @Override
  public CommandResult<Void> runCommand() {
    orgId = PlatformContext.requireOrg(orgId);
    Path publicKeyFile = keyFile.resolveSibling(keyFile.getFileName() + ".pub");
    requireAbsent(keyFile);
    requireAbsent(publicKeyFile);

    PrivatePublicKeyPair pair;
    try (PlatformClients client = PlatformClients.fromConfig()) {
      pair = new ProjectRepositoryApi(client).generateKeyPair(orgId);
    }
    writePrivateKey(keyFile, pair.getPrivateKey());
    writePublicKey(publicKeyFile, pair.getPublicKey());
    System.out.println(
        msg.projectSshKeyPairWritten(keyFile.toString(), publicKeyFile.toString()));
    return new CommandResult<>(null);
  }

  private static void requireAbsent(Path path) {
    if (Files.exists(path)) {
      throw new CliException(msg.projectSshKeyFileExists(path.toString()));
    }
  }

  private static void writePrivateKey(Path path, String key) {
    try {
      try {
        Files.createFile(path, PosixFilePermissions.asFileAttribute(
            PosixFilePermissions.fromString("rw-------")));
      } catch (UnsupportedOperationException nonPosix) {
        Files.createFile(path);
      }
      Files.writeString(path, withTrailingNewline(key));
    } catch (IOException e) {
      throw new CliException(
          msg.projectSshKeyFileWriteFailed(path.toString(), e.getMessage()), e);
    }
  }

  private static void writePublicKey(Path path, String key) {
    try {
      Files.writeString(path, withTrailingNewline(key), StandardOpenOption.CREATE_NEW);
    } catch (IOException e) {
      throw new CliException(
          msg.projectSshKeyFileWriteFailed(path.toString(), e.getMessage()), e);
    }
  }

  private static String withTrailingNewline(String key) {
    return key.endsWith("\n") ? key : key + "\n";
  }
}
