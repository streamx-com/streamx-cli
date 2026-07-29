package com.streamx.cli.commands.org;

import com.streamx.cli.commands.org.clusters.ClustersCommand;
import com.streamx.cli.commands.org.create.CreateCommand;
import com.streamx.cli.commands.org.delete.DeleteCommand;
import com.streamx.cli.commands.org.get.GetCommand;
import com.streamx.cli.commands.org.list.ListCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "org",
    header = "Manage StreamX organizations",
    subcommands = {
        ClustersCommand.class,
        CreateCommand.class,
        DeleteCommand.class,
        GetCommand.class,
        ListCommand.class
    }
)
public class OrgCommand extends AbstractCommandGroup {
}
