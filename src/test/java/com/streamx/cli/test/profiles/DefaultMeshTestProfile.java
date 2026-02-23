package com.streamx.cli.test.profiles;

import com.streamx.cli.test.MeshTestEnv;
import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class DefaultMeshTestProfile implements QuarkusTestProfile {
  public static final String PROFILE_NAME = "mesh-test";

  @Override
  public String getConfigProfile() {
    return PROFILE_NAME;
  }

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of(MeshTestEnv.MESH_PATH_CONFIG, "target/test-classes/mesh.yaml");
  }
}