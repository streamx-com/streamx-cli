package com.streamx.cli.mesh;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static com.streamx.cli.util.Output.print;

import com.streamx.runner.event.ContainerFailed;
import com.streamx.runner.event.ContainerStarted;
import com.streamx.runner.event.ContainerStopped;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class ContainerWatcher {

  void onContainerStarted(@Observes ContainerStarted event) {
    print(msg.dockerContainerStarted(event.containerName()));
  }

  void onContainerStopped(@Observes ContainerStopped event) {
    print(msg.dockerContainerStopped(event.containerName()));
  }

  void onContainerFailed(@Observes ContainerFailed event) {
    print(msg.dockerContainerFailed(event.containerName()));
  }
}
