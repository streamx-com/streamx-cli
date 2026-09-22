package com.streamx.cli.commands.org.get;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.DetailsView;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.OrganizationsApi;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.generated.model.Organization;
import picocli.CommandLine;

@CommandLine.Command(
    name = "get",
    header = "Display an organization"
)
public class GetCommand extends AbstractCommand<Organization> {
  @CommandLine.Parameters(
      index = "0",
      arity = "0..1",
      description = "Organization ID (defaults to the current organization)",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @Override
  public String getTextOutput(CommandResult<Organization> result) {
    Organization organization = result.getData();
    return new DetailsView()
        .row("id", organization.getId())
        .row("name", organization.getName())
        .row("role", organization.getRole() == null ? null : organization.getRole().getName())
        .row("projectsNumber", organization.getProjectsNumber())
        .row("state", organization.getState())
        .render();
  }

  @Override
  public CommandResult<Organization> runCommand() {
    orgId = PlatformContext.requireOrg(orgId);
    try (PlatformClients client = PlatformClients.fromConfig()) {
      return new CommandResult<>(new OrganizationsApi(client).get(orgId));
    }
  }
}
