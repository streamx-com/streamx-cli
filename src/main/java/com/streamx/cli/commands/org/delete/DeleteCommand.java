package com.streamx.cli.commands.org.delete;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrganizationsApi;
import com.streamx.cli.platform.PlatformApiClient;
import picocli.CommandLine;

@CommandLine.Command(
    name = "delete",
    header = "Delete an organization"
)
public class DeleteCommand extends AbstractSilentCommand {
  @CommandLine.Parameters(index = "0", description = "Organization ID")
  public String orgId;

  @Override
  public CommandResult<Void> runCommand() {
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      new OrganizationsApi(client).delete(orgId);
    }
    System.out.println(msg.orgDeleted(orgId));
    return new CommandResult<>(null);
  }
}
