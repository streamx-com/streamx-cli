package com.streamx.cli.commands.org;

import com.streamx.cli.framework.DetailsView;
import com.streamx.cli.platform.generated.model.Organization;

/** The detail view of an organization, shared by {@code org get} and {@code org create}. */
public final class OrganizationDetails {

  private OrganizationDetails() {
  }

  public static String describe(Organization organization) {
    return new DetailsView()
        .row("id", organization.getId())
        .row("name", organization.getName())
        .row("role", organization.getRole() == null ? null : organization.getRole().getName())
        .row("projectsNumber", organization.getProjectsNumber())
        .row("state", organization.getState())
        .render();
  }
}
