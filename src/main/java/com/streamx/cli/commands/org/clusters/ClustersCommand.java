package com.streamx.cli.commands.org.clusters;

import com.streamx.cli.commands.org.clusters.list.ListCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "clusters",
    header = "Inspect organization clusters",
    subcommands = {
        ListCommand.class
    }
)
public class ClustersCommand extends AbstractCommandGroup {
}
