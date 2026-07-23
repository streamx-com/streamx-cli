package com.streamx.cli.commands.profile.project;

import com.streamx.cli.commands.profile.project.current.CurrentCommand;
import com.streamx.cli.commands.profile.project.unset.UnsetCommand;
import com.streamx.cli.commands.profile.project.use.UseCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "project",
    header = "Manage the current project of the active profile",
    subcommands = {
        UseCommand.class,
        CurrentCommand.class,
        UnsetCommand.class
    }
)
public class ProjectCommand extends AbstractCommandGroup {
}
