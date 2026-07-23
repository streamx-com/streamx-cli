package com.streamx.cli.commands.profile;

import com.streamx.cli.commands.profile.configure.ConfigureCommand;
import com.streamx.cli.commands.profile.create.CreateCommand;
import com.streamx.cli.commands.profile.current.CurrentCommand;
import com.streamx.cli.commands.profile.delete.DeleteCommand;
import com.streamx.cli.commands.profile.list.ListCommand;
import com.streamx.cli.commands.profile.org.OrgCommand;
import com.streamx.cli.commands.profile.project.ProjectCommand;
import com.streamx.cli.commands.profile.use.UseCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "profile",
    header = "Manage StreamX profiles (bundled settings, event templates and login "
        + "per environment)",
    subcommands = {
        ListCommand.class,
        CreateCommand.class,
        ConfigureCommand.class,
        UseCommand.class,
        CurrentCommand.class,
        OrgCommand.class,
        ProjectCommand.class,
        DeleteCommand.class
    }
)
public class ProfileCommand extends AbstractCommandGroup {
}
