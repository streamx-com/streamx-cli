package com.streamx.cli.commands.org.invitations;

import com.streamx.cli.commands.org.invitations.accept.AcceptCommand;
import com.streamx.cli.commands.org.invitations.cancel.CancelCommand;
import com.streamx.cli.commands.org.invitations.create.CreateCommand;
import com.streamx.cli.commands.org.invitations.list.ListCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "invitations",
    header = "Manage organization invitations",
    subcommands = {
        AcceptCommand.class,
        CancelCommand.class,
        CreateCommand.class,
        ListCommand.class
    }
)
public class InvitationsCommand extends AbstractCommandGroup {
}
