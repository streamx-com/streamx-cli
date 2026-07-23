package com.streamx.cli.commands.info;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
public record InfoResult(
    Cli cli,
    Profile profile,
    List<Setting> settings,
    Login login,
    List<Probe> connectivity,
    List<String> warnings
) {

  @RegisterForReflection
  public record Cli(String version, String runtime, String home, String homeSource) {
  }

  @RegisterForReflection
  public record Profile(String active, String source, boolean exists, String settingsFile) {
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
