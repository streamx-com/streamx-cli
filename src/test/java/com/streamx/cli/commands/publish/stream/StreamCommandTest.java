package com.streamx.cli.commands.publish.stream;

import com.streamx.cli.test.MeshTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Test;

@QuarkusMainTest
@TestProfile(MeshTestProfile.class)
public class StreamCommandTest {

  @Test
  void shouldStreamSingleEvent(QuarkusMainLauncher launcher) {
    LaunchResult result = launcher.launch("publish", "stream", "hello");
    System.out.println(result.exitCode());
  }
}