package com.streamx.cli.test.annotation;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DockerAvailableCondition implements ExecutionCondition {

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    try {
      Process process = new ProcessBuilder("docker", "info")
          .redirectErrorStream(true)
          .start();
      boolean exited = process.waitFor(5, TimeUnit.SECONDS);
      if (exited && process.exitValue() == 0) {
        return ConditionEvaluationResult.enabled("Docker is available");
      }
    } catch (Exception ignored) {
      // ignore
    }
    return ConditionEvaluationResult.disabled("Docker is not available");
  }
}