package com.streamx.cli.commands.profile.org;

import com.streamx.cli.commands.profile.org.current.CurrentCommand;
import com.streamx.cli.commands.profile.org.unset.UnsetCommand;
import com.streamx.cli.commands.profile.org.use.UseCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "org",
    header = "Manage the current organization of the active profile",
    subcommands = {
        UseCommand.class,
        CurrentCommand.class,
        UnsetCommand.class
    }
)
public class OrgCommand extends AbstractCommandGroup {
}
