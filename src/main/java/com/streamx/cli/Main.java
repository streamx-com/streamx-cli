package com.streamx.cli;

import com.streamx.cli.commands.StreamxCommand;
import com.streamx.cli.framework.ShortErrorMessageHandler;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import picocli.CommandLine;

@QuarkusMain
public class Main implements QuarkusApplication {

  @Inject
  CommandLine.IFactory factory;

  @Override
  public int run(String... args) throws Exception {
    CommandLine commandLine = new CommandLine(new StreamxCommand(), factory)
        .setParameterExceptionHandler(new ShortErrorMessageHandler())
        .setExpandAtFiles(false)
        .setExecutionStrategy(new CommandLine.RunLast());

    return commandLine.execute(args);
  }
}