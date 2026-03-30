package com.streamx.cli.mesh;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.streamx.cli.framework.CliException;
import com.streamx.mesh.model.ServiceMesh;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MeshDefinitionResolverInterpolationTest {

  private static final Path TEST_MESH_PATH =
      Path.of("target/test-classes/mesh-interpolated.yaml");

  @Inject
  MeshDefinitionResolver uut;

  @BeforeEach
  void clearSystemProperties() {
    System.clearProperty("config.image.interpolated");
    System.clearProperty("config.source.interpolated");
    System.clearProperty("STREAMX_OWNER_SERVICE_NAME");
  }

  @Test
  void shouldFailWithUndefinedProperty() {
    System.setProperty("STREAMX_OWNER_SERVICE_NAME", "test-owner");
    assertThatThrownBy(() -> uut.resolve(TEST_MESH_PATH))
        .isInstanceOf(JsonMappingException.class)
        .hasRootCauseInstanceOf(CliException.class)
        .hasRootCauseMessage(msg.unresolvedProperty(
            "config.image.interpolated",
            "${config.image.interpolated}"
        ));
  }

  @Test
  void shouldKeepPlaceholderForUndefinedEnvVariable() throws IOException {
    System.setProperty("config.image.interpolated", "image-1");
    ServiceMesh result = uut.resolve(TEST_MESH_PATH);
    String envValue = result.getEdge().get("web-server-sink")
        .getContainers().get("sink").getEnvironment()
        .get("QUARKUS_ELASTICSEARCH_HOSTS");
    assertThat(envValue).isEqualTo("${STREAMX_OWNER_SERVICE_NAME}.opensearch:9201");
  }

  @Test
  void shouldResolveWithMandatoryPropertyDefinedAndOptionalPropertyUndefined() throws IOException {
    System.setProperty("config.image.interpolated", "image-1");
    System.setProperty("STREAMX_OWNER_SERVICE_NAME", "test-owner");

    ServiceMesh result = uut.resolve(TEST_MESH_PATH);
    assertSinkImage(result, "image-1");
    assertSourceRef(result, "inbox.pages");
  }

  @Test
  void shouldResolveWithMandatoryAndOptionalPropertiesDefined() throws IOException {
    System.setProperty("config.image.interpolated", "image-1");
    System.setProperty("config.source.interpolated", "source-1");
    System.setProperty("STREAMX_OWNER_SERVICE_NAME", "test-owner");

    ServiceMesh result = uut.resolve(TEST_MESH_PATH);
    assertSinkImage(result, "image-1");
    assertSourceRef(result, "source-1");
  }

  private static void assertSinkImage(ServiceMesh result, String expected) {
    String actual = result
        .getDescriptors()
        .get("web-server-sink")
        .getContainers()
        .get("sink")
        .getImage();
    assertThat(actual).isEqualTo(expected);
  }

  private static void assertSourceRef(ServiceMesh result, String expected) {
    String actual = result
        .getSources()
        .get("cli")
        .getOutgoing()
        .getFirst()
        .getRef();
    assertThat(actual).isEqualTo(expected);
  }
}
