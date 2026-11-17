package com.streamx.cli.commands.project.domains;

import com.streamx.cli.commands.project.domains.add.AddCommand;
import com.streamx.cli.commands.project.domains.get.GetCommand;
import com.streamx.cli.commands.project.domains.getzonefile.GetZoneFileCommand;
import com.streamx.cli.commands.project.domains.list.ListCommand;
import com.streamx.cli.commands.project.domains.recheckdns.RecheckDnsCommand;
import com.streamx.cli.commands.project.domains.remove.RemoveCommand;
import com.streamx.cli.commands.project.domains.status.StatusCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "domains",
    header = "Manage the custom domains of a project",
    description = {
        "Manage custom domains and check their setup. "
            + "Use 'add' to display the DNS records you need to create."
    },
    footer = {
        "",
        "Examples:",
        "  streamx project domains add shop shop.example.com",
        "  # create the two records it prints at your DNS provider, then:",
        "  streamx project domains recheck-dns shop.example.com --wait"
    },
    subcommands = {
        ListCommand.class,
        GetCommand.class,
        AddCommand.class,
        RemoveCommand.class,
        StatusCommand.class,
        GetZoneFileCommand.class,
        RecheckDnsCommand.class
    }
)
public class DomainsCommand extends AbstractCommandGroup {
}
