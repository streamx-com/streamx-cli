package com.streamx.cli.commands.settings.eventtemplates;

import com.streamx.cli.commands.settings.eventtemplates.copy.CopyCommand;
import com.streamx.cli.commands.settings.eventtemplates.create.CreateCommand;
import com.streamx.cli.commands.settings.eventtemplates.delete.DeleteCommand;
import com.streamx.cli.commands.settings.eventtemplates.edit.EditCommand;
import com.streamx.cli.commands.settings.eventtemplates.get.GetCommand;
import com.streamx.cli.commands.settings.eventtemplates.list.ListCommand;
import com.streamx.cli.commands.settings.eventtemplates.placeholders.PlaceholdersCommand;
import com.streamx.cli.commands.settings.eventtemplates.register.RegisterCommand;
import com.streamx.cli.commands.settings.eventtemplates.rename.RenameCommand;
import com.streamx.cli.commands.settings.eventtemplates.resetdefaulttemplates.ResetDefaultTemplatesCommand;
import com.streamx.cli.commands.settings.eventtemplates.unregister.UnregisterCommand;
import com.streamx.cli.commands.settings.eventtemplates.validate.ValidateCommand;
import com.streamx.cli.commands.settings.eventtemplates.which.WhichCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "event-templates",
    header = "Manage StreamX event templates",
    subcommands = {
        CopyCommand.class,
        CreateCommand.class,
        DeleteCommand.class,
        EditCommand.class,
        GetCommand.class,
        ListCommand.class,
        PlaceholdersCommand.class,
        RegisterCommand.class,
        RenameCommand.class,
        ResetDefaultTemplatesCommand.class,
        UnregisterCommand.class,
        ValidateCommand.class,
        WhichCommand.class
    }
)
public class EventTemplatesCommand extends AbstractCommandGroup {
}
