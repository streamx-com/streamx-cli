package com.streamx.cli.commands.completion;

import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "completion",
    header = "Generate shell completion scripts",
    subcommands = {
        BashCompletionCommand.class,
        ZshCompletionCommand.class
    }
)
public class CompletionCommand extends AbstractCommandGroup {
}
