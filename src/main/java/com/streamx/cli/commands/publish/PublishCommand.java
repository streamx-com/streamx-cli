package com.streamx.cli.commands.publish;

import com.streamx.cli.commands.publish.stream.StreamCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "publish",
    mixinStandardHelpOptions = true,
    subcommands = {
        StreamCommand.class,
    }
)
public class PublishCommand extends AbstractCommandGroup {
}
