package com.streamx.cli.commands.org.list;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.TextTable;
import com.streamx.cli.platform.OrganizationsApi;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.generated.model.Organization;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    header = "List organizations you have access to"
)
public class ListCommand extends AbstractCommand<List<Organization>> {

  @CommandLine.Option(
      names = {"-q", "--quiet"},
      description = "Only display organization IDs, one per line (for piping to xargs)"
  )
  public boolean quiet;

  @Override
  public String getTextOutput(CommandResult<List<Organization>> result) {
    List<Organization> organizations = result.getData();

    if (quiet) {
      return organizations.stream()
          .map(Organization::getId)
          .filter(Objects::nonNull)
          .collect(Collectors.joining("\n"));
    }

    if (organizations.isEmpty()) {
      return msg.orgListEmpty();
    }

    return TextTable.render(
        List.of("ID", "NAME", "ROLE", "PROJECTS", "STATE"),
        organizations.stream()
            .map(organization -> Arrays.asList(
                organization.getId(),
                organization.getName(),
                organization.getRole() == null ? null : organization.getRole().getName(),
                organization.getProjectsNumber(),
                organization.getState()))
            .toList());
  }

  @Override
  public CommandResult<List<Organization>> runCommand() {
    try (PlatformClients client = PlatformClients.fromConfig()) {
      return new CommandResult<>(new OrganizationsApi(client).list());
    }
  }
}
