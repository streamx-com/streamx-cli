package com.streamx.cli.platform;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.CliException;

/**
 * Resolves the organization/project a command operates on.
 *
 * <pre>
 *   explicit argument > STREAMX_ORG / STREAMX_PROJECT > profile's current-org/current-project
 * </pre>
 *
 * The env vars are invocation-scoped overrides (CI, scripts) and deliberately not bound to a
 * profile; the files are the per-profile persisted context written by {@code org use} and
 * {@code project use}.
 */
public final class PlatformContext {

  public static final String STREAMX_ORG = "STREAMX_ORG";
  public static final String STREAMX_PROJECT = "STREAMX_PROJECT";

  private PlatformContext() {
  }

  public record OrgValue(String org, String value) {
  }

  public record OrgProject(String org, String project) {
  }

  public static String effectiveOrg() {
    String env = override(STREAMX_ORG);
    return env != null ? env : StreamxHome.readCurrentOrg();
  }

  public static String effectiveProject() {
    String env = override(STREAMX_PROJECT);
    return env != null ? env : StreamxHome.readCurrentProject();
  }

  public static String effectiveOrgSource() {
    if (override(STREAMX_ORG) != null) {
      return "from the STREAMX_ORG environment variable";
    }
    return StreamxHome.readCurrentOrg() != null ? "from the current-org file" : null;
  }

  public static String effectiveProjectSource() {
    if (override(STREAMX_PROJECT) != null) {
      return "from the STREAMX_PROJECT environment variable";
    }
    return StreamxHome.readCurrentProject() != null ? "from the current-project file" : null;
  }

  public static String requireOrg(String orgArg) {
    if (orgArg != null && !orgArg.isBlank()) {
      return orgArg;
    }
    String effective = effectiveOrg();
    if (effective == null) {
      throw new CliException(msg.noOrgContext());
    }
    return effective;
  }

  public static OrgValue orgAndValue(String orgArg, String valueArg, String valueLabel) {
    if (valueArg != null) {
      return new OrgValue(requireOrg(orgArg), valueArg);
    }
    if (orgArg != null) {
      return new OrgValue(requireOrg(null), orgArg);
    }
    throw new CliException(msg.missingRequiredArgument(valueLabel));
  }

  public static OrgProject orgAndProject(String orgArg, String projectArg) {
    if (projectArg != null) {
      return new OrgProject(requireOrg(orgArg), projectArg);
    }
    if (orgArg != null) {
      return new OrgProject(requireOrg(null), orgArg);
    }
    String project = effectiveProject();
    if (project == null) {
      throw new CliException(msg.noProjectContext());
    }
    return new OrgProject(requireOrg(null), project);
  }

  public static String setCurrentOrg(String orgId) {
    try {
      String previousOrg = StreamxHome.readCurrentOrg();
      String currentProject = StreamxHome.readCurrentProject();
      StreamxHome.writeCurrentOrg(orgId);
      if (currentProject != null && previousOrg != null && !previousOrg.equals(orgId)) {
        StreamxHome.clearCurrentProject();
        return currentProject;
      }
      return null;
    } catch (java.io.IOException e) {
      throw new CliException(e.getMessage(), e);
    }
  }

  public static void setCurrentProject(String projectId) {
    if (StreamxHome.readCurrentOrg() == null) {
      throw new CliException(msg.noCurrentOrg());
    }
    try {
      StreamxHome.writeCurrentProject(projectId);
    } catch (java.io.IOException e) {
      throw new CliException(e.getMessage(), e);
    }
  }

  private static String override(String name) {
    String value = System.getProperty(name);
    if (value == null || value.isBlank()) {
      value = System.getenv(name);
    }
    return value == null || value.isBlank() ? null : value;
  }
}
