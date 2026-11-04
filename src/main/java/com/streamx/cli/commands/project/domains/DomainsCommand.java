package com.streamx.cli.commands.project.domains;

import com.streamx.cli.commands.project.domains.add.AddCommand;
import com.streamx.cli.commands.project.domains.get.GetCommand;
import com.streamx.cli.commands.project.domains.list.ListCommand;
import com.streamx.cli.commands.project.domains.records.RecordsCommand;
import com.streamx.cli.commands.project.domains.remove.RemoveCommand;
import com.streamx.cli.commands.project.domains.status.StatusCommand;
import com.streamx.cli.commands.project.domains.verify.VerifyCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "domains",
    header = "Manage the custom domains of a project",
    description = {
        "A project serves a custom domain once two DNS records exist for it: a TXT record that "
            + "proves you control the hostname, and a CNAME that lets StreamX obtain its TLS "
            + "certificate. 'add' prints both."
    },
    footer = {
        "",
        "Examples:",
        "  streamx project domains add shop.example.com",
        "  # create the two records it prints at your DNS provider, then:",
        "  streamx project domains verify shop.example.com --wait"
    },
    subcommands = {
        ListCommand.class,
        GetCommand.class,
        AddCommand.class,
        RemoveCommand.class,
        RecordsCommand.class,
        StatusCommand.class,
        VerifyCommand.class
    }
)
public class DomainsCommand extends AbstractCommandGroup {
}
