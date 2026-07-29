package com.streamx.cli.commands.context.project;

import com.streamx.cli.commands.context.project.current.CurrentCommand;
import com.streamx.cli.commands.context.project.unset.UnsetCommand;
import com.streamx.cli.commands.context.project.use.UseCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "project",
    header = "Manage the current project of the active context",
    subcommands = {
        UseCommand.class,
        CurrentCommand.class,
        UnsetCommand.class
    }
)
public class ProjectCommand extends AbstractCommandGroup {
}
