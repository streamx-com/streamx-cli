package com.streamx.cli.commands.context;

import com.streamx.cli.commands.context.create.CreateCommand;
import com.streamx.cli.commands.context.current.CurrentCommand;
import com.streamx.cli.commands.context.delete.DeleteCommand;
import com.streamx.cli.commands.context.list.ListCommand;
import com.streamx.cli.commands.context.use.UseCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "context",
    header = "Manage StreamX contexts (bundled settings, event templates and login "
        + "per environment)",
    subcommands = {
        ListCommand.class,
        CreateCommand.class,
        UseCommand.class,
        CurrentCommand.class,
        DeleteCommand.class
    }
)
public class ContextCommand extends AbstractCommandGroup {
}
