package com.streamx.cli.commands;

import com.streamx.cli.commands.local.LocalCommand;
import com.streamx.cli.commands.publish.PublishCommand;
import com.streamx.cli.commands.settings.SettingsCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import com.streamx.cli.util.VersionProvider;
import picocli.CommandLine;

@CommandLine.Command(
    name = "streamx",
    mixinStandardHelpOptions = true,
    header = "StreamX CLI. More info at https://streamx.com",
    subcommands = {
        LocalCommand.class,
        SettingsCommand.class,
        PublishCommand.class
    },
    versionProvider = VersionProvider.class
)
public class StreamxCommand extends AbstractCommandGroup {
}
