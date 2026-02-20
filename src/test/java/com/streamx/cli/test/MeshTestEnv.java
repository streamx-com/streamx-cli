package com.streamx.cli.test;

import com.streamx.cli.mesh.MeshManager;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.nio.file.Paths;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
@IfBuildProfile(MeshTestProfile.PROFILE_NAME)
public class MeshTestEnv {

  private static final Logger LOG = Logger.getLogger(MeshTestEnv.class);

  public static final String MESH_PATH_CONFIG = "test.mesh.path";

  @Inject
  MeshManager meshManager;

  @ConfigProperty(name = MESH_PATH_CONFIG)
  String meshPath;

  void onStart(@Observes StartupEvent ev) {
    var path = Paths.get(meshPath);
    meshManager.initializeMesh(path);
    meshManager.initializeRunMode(path);
    meshManager.start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    LOG.info("Stopping mesh after tests...");

    try {
      meshManager.stop();
    } catch (Exception e) {
      LOG.error("Error during stopping mesh", e);
    }
  }
}