package com.streamx.cli.commands.settings;

import com.streamx.cli.commands.settings.get.GetCommand;
import com.streamx.cli.commands.settings.list.ListCommand;
import com.streamx.cli.commands.settings.set.SetCommand;
import com.streamx.cli.commands.settings.unset.UnsetCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "settings",
    header = "Modify StreamX settings",
    subcommands = {
        GetCommand.class,
        ListCommand.class,
        SetCommand.class,
        UnsetCommand.class
    }
)
public class SettingsCommand extends AbstractCommandGroup {
}
