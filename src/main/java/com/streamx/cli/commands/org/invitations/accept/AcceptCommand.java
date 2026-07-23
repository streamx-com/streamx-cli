package com.streamx.cli.commands.org.invitations.accept;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CliException;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrganizationInvitationsApi;
import com.streamx.cli.platform.PlatformApiClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import picocli.CommandLine;

@CommandLine.Command(
    name = "accept",
    header = "Accept an invitation to an organization",
    description = {
        "The invitation token is read from standard input, or from --token-file.",
        "It is not taken as an argument: that would leave a credential in the shell",
        "history and expose it to anyone listing processes.",
        "",
        "  streamx org invitations accept <orgId> --token-file ./token.txt",
        "  pbpaste | streamx org invitations accept <orgId>"
    }
)
public class AcceptCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(index = "0", description = "Organization ID")
  public String orgId;

  @CommandLine.Option(
      names = "--token-file",
      description = "File holding the invitation token; defaults to reading standard input"
  )
  public Path tokenFile;

  @Override
  public CommandResult<Void> runCommand() {
    String token = readToken();

    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      new OrganizationInvitationsApi(client).accept(orgId, token);
    }
    System.out.println(msg.orgInvitationAccepted());
    return new CommandResult<>(null);
  }

  private String readToken() {
    String token;
    try {
      token = tokenFile != null
          ? Files.readString(tokenFile, StandardCharsets.UTF_8)
          : new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new CliException(msg.orgInvitationTokenRequired(), e);
    }

    token = token.strip();
    if (token.isEmpty()) {
      throw new CliException(msg.orgInvitationTokenRequired());
    }
    return token;
  }
}
