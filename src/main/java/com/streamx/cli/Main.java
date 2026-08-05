package com.streamx.cli;

import com.streamx.cli.commands.StreamxCommand;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.ShortErrorMessageHandler;
import com.streamx.cli.framework.SynopsisHelper;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import java.util.List;
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
        .setUsageHelpAutoWidth(true)
        .setExecutionStrategy(parseResult -> {
          List<CommandLine> parsed = parseResult.asCommandLineList();
          Object lastCommand = parsed.get(parsed.size() - 1).getCommand();
          if (lastCommand instanceof AbstractCommand<?> abstractCommand) {
            try {
              abstractCommand.populateStreamxHome(parsed);
              // -H/--context are applied now; refresh the root help header to reflect them.
              SynopsisHelper.applyRootUsageLayout(parsed.get(0));
            } catch (Exception e) {
              return abstractCommand.handleExecutionError(e);
            }
          }
          return new CommandLine.RunLast().execute(parseResult);
        });

    SynopsisHelper.applyCustomSynopses(commandLine);
    SynopsisHelper.applyRootUsageLayout(commandLine);

    return commandLine.execute(args);
  }
}
