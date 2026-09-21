package com.streamx.cli.commands.info;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
public record InfoResult(
    Cli cli,
    Context context,
    List<Setting> settings,
    Login login,
    List<Probe> connectivity,
    List<String> warnings
) {

  @RegisterForReflection
  public record Cli(String version, String runtime, String home, String homeSource) {
  }

  @RegisterForReflection
  public record Context(
      String active,
      String source,
      boolean exists,
      String settingsFile,
      String currentOrg,
      String currentOrgSource,
      String currentProject,
      String currentProjectSource) {
  }

  @RegisterForReflection
  public record Setting(String key, String value, String source) {
  }

  @RegisterForReflection
  public record Login(String state, String user, String expiresAt, String issuer) {
  }

  @RegisterForReflection
  public record Probe(String name, String target, String status, Long latencyMs, String detail) {
  }
}
