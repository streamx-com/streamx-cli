package com.streamx.cli.platform;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.CliException;

/**
 * Resolves the organization/project a command operates on.
 *
 * <pre>
 *   explicit argument > STREAMX_ORG / STREAMX_PROJECT > context's current-org/current-project
 * </pre>
 *
 * The env vars are invocation-scoped overrides (CI, scripts) and deliberately not bound to a
 * context; the files are the per-context persisted context written by {@code org use} and
 * {@code project use}.
 */
public final class PlatformContext {

  public static final String STREAMX_ORG = "STREAMX_ORG";

  private PlatformContext() {
  }

  public static String effectiveOrg() {
    String env = override(STREAMX_ORG);
    return env != null ? env : StreamxHome.readCurrentOrg();
  }

  public static String completionOrg(String orgArg) {
    return orgArg == null || orgArg.isBlank() ? effectiveOrg() : orgArg;
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

  private static String override(String name) {
    String value = System.getProperty(name);
    if (value == null || value.isBlank()) {
      value = System.getenv(name);
    }
    return value == null || value.isBlank() ? null : value;
  }
}
