package com.streamx.cli.commands.org.create;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrganizationsApi;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.generated.model.Organization;
import picocli.CommandLine;

@CommandLine.Command(
    name = "create",
    header = "Create an organization"
)
public class CreateCommand extends AbstractCommand<Organization> {
  @CommandLine.Parameters(index = "0", description = "Organization name")
  public String name;

  @Override
  public String getTextOutput(CommandResult<Organization> result) {
    return msg.orgCreated(name, result.getData().getId());
  }

  @Override
  public CommandResult<Organization> runCommand() {
    try (PlatformClients client = PlatformClients.fromConfig()) {
      return new CommandResult<>(new OrganizationsApi(client).create(name));
    }
  }
}
