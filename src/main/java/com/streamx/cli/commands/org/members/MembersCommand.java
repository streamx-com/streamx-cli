package com.streamx.cli.commands.org.members;

import com.streamx.cli.commands.org.members.add.AddCommand;
import com.streamx.cli.commands.org.members.list.ListCommand;
import com.streamx.cli.commands.org.members.remove.RemoveCommand;
import com.streamx.cli.commands.org.members.setrole.SetRoleCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "members",
    header = "Manage organization members",
    subcommands = {
        AddCommand.class,
        ListCommand.class,
        RemoveCommand.class,
        SetRoleCommand.class
    }
)
public class MembersCommand extends AbstractCommandGroup {
}
