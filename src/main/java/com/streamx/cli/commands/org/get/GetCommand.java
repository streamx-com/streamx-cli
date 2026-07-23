package com.streamx.cli.commands.org.get;

import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.OrgIdCompletionCandidates;
import com.streamx.cli.platform.Organization;
import com.streamx.cli.platform.OrganizationsApi;
import com.streamx.cli.platform.PlatformApiClient;
import picocli.CommandLine;

@CommandLine.Command(
    name = "get",
    header = "Display an organization"
)
public class GetCommand extends AbstractCommand<Organization> {
  @CommandLine.Parameters(
      index = "0",
      description = "Organization ID",
      completionCandidates = OrgIdCompletionCandidates.class
  )
  public String orgId;

  @Override
  public String getTextOutput(CommandResult<Organization> result) {
    Organization organization = result.getData();
    return """
        id             = %s
        name           = %s
        role           = %s
        projectsNumber = %s
        state          = %s"""
        .formatted(
            orDash(organization.id()),
            orDash(organization.name()),
            orDash(organization.role()),
            orDash(organization.projectsNumber()),
            orDash(organization.state()));
  }

  private static String orDash(String value) {
    return value == null ? "-" : value;
  }

  @Override
  public CommandResult<Organization> runCommand() {
    try (PlatformApiClient client = PlatformApiClient.fromConfig()) {
      return new CommandResult<>(new OrganizationsApi(client).get(orgId));
    }
  }
}
