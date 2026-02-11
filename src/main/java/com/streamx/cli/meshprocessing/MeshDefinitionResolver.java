package com.streamx.cli.meshprocessing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.cli.interpolation.Interpolating;
import com.streamx.mesh.model.ServiceMesh;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;

@ApplicationScoped
public class MeshDefinitionResolver {

  @Inject
  @Interpolating
  ObjectMapper objectMapper;

  public ServiceMesh resolve(Path meshPath) throws IOException {
    return objectMapper.readValue(meshPath.toFile(), ServiceMesh.class);
  }
}
