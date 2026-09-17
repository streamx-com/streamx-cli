package com.streamx.cli.commands.project.clusters;

import com.streamx.cli.commands.project.clusters.disable.DisableCommand;
import com.streamx.cli.commands.project.clusters.enable.EnableCommand;
import com.streamx.cli.commands.project.clusters.list.ListCommand;
import com.streamx.cli.commands.project.clusters.set.SetCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "clusters",
    header = "Manage the clusters a project runs on",
    subcommands = {
        ListCommand.class,
        SetCommand.class,
        EnableCommand.class,
        DisableCommand.class
    }
)
public class ClustersCommand extends AbstractCommandGroup {
}
