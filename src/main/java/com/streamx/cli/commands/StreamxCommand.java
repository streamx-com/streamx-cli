package com.streamx.cli.commands;

import com.streamx.cli.commands.completion.CompleteNonDefaultTemplateIdsCommand;
import com.streamx.cli.commands.completion.CompleteRegisteredTemplateIdsCommand;
import com.streamx.cli.commands.completion.CompleteSettingsKeysCommand;
import com.streamx.cli.commands.completion.CompleteSettingsSetKeysCommand;
import com.streamx.cli.commands.completion.CompleteTemplateIdsCommand;
import com.streamx.cli.commands.completion.CompletionCommand;
import com.streamx.cli.commands.local.LocalCommand;
import com.streamx.cli.commands.publish.PublishCommand;
import com.streamx.cli.commands.settings.SettingsCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "streamx",
    header = "StreamX CLI. More info at https://streamx.com",
    subcommands = {
        LocalCommand.class,
        SettingsCommand.class,
        PublishCommand.class,
        CompletionCommand.class,
        CompleteTemplateIdsCommand.class,
        CompleteRegisteredTemplateIdsCommand.class,
        CompleteNonDefaultTemplateIdsCommand.class,
        CompleteSettingsKeysCommand.class,
        CompleteSettingsSetKeysCommand.class
    }
)
public class StreamxCommand extends AbstractCommandGroup {
}
