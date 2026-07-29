package com.streamx.cli.commands.context.org;

import com.streamx.cli.commands.context.org.current.CurrentCommand;
import com.streamx.cli.commands.context.org.unset.UnsetCommand;
import com.streamx.cli.commands.context.org.use.UseCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "org",
    header = "Manage the current organization of the active context",
    subcommands = {
        UseCommand.class,
        CurrentCommand.class,
        UnsetCommand.class
    }
)
public class OrgCommand extends AbstractCommandGroup {
}
