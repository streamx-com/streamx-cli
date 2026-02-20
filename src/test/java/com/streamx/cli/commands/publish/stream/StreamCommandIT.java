package com.streamx.cli.commands.publish.stream;

import com.streamx.cli.test.AbstractCommandIT;
import com.streamx.cli.test.MeshTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(MeshTestProfile.class)
public class StreamCommandIT extends AbstractCommandIT {

  @Test
  void shouldStreamSingleEvent() throws Exception {
    String exampleEvent = """
            {
              "specversion": "1.0",
              "id": "Bar & Serving Carts",
              "source": "streamx-commerce-accelerator",
              "type": "com.streamx.blueprints.data.published.v1",
              "datacontenttype": "application/json",
              "subject": "cat:Bar & Serving Carts",
              "time": "2026-01-01T00:00:00.000000Z",
              "data": {
                "content": "{\\"id\\":\\"Bar & Serving Carts\\"}",
                "type": "data/category"
              }
            }
            """;

    ProcessResult result = execWithStdin(exampleEvent, "publish", "stream", "-v");
    result.assertSuccess();
  }
}