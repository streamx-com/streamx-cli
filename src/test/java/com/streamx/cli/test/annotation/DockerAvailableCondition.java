package com.streamx.cli.test.annotation;

import com.github.dockerjava.api.DockerClient;
import com.streamx.runner.docker.DockerClientFactory;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DockerAvailableCondition implements ExecutionCondition {

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    try (DockerClient docker = DockerClientFactory.create()) {
      docker.pingCmd().exec();
      return ConditionEvaluationResult.enabled("Docker is available");
    } catch (Exception ignored) {
      // ignore
    }
    return ConditionEvaluationResult.disabled("Docker is not available");
  }
}