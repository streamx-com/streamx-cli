package com.streamx.cli.commands.org.create;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrganizationsApi;
import com.streamx.cli.platform.PlatformClients;
import picocli.CommandLine;

@CommandLine.Command(
    name = "create",
    header = "Create an organization"
)
public class CreateCommand extends AbstractSilentCommand {
  @CommandLine.Parameters(index = "0", description = "Organization name")
  public String name;

  @Override
  public CommandResult<Void> runCommand() {
    try (PlatformClients client = PlatformClients.fromConfig()) {
      new OrganizationsApi(client).create(name);
    }
    System.out.println(msg.orgCreated(name));
    return new CommandResult<>(null);
  }
}
