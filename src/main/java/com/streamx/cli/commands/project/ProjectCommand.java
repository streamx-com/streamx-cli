package com.streamx.cli.commands.project;

import com.streamx.cli.commands.project.clusters.ClustersCommand;
import com.streamx.cli.commands.project.create.CreateCommand;
import com.streamx.cli.commands.project.delete.DeleteCommand;
import com.streamx.cli.commands.project.get.GetCommand;
import com.streamx.cli.commands.project.list.ListCommand;
import com.streamx.cli.commands.project.pendingchanges.PendingChangesCommand;
import com.streamx.cli.commands.project.repo.RepoCommand;
import com.streamx.cli.commands.project.status.StatusCommand;
import com.streamx.cli.commands.project.update.UpdateCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "project",
    header = "Manage StreamX projects",
    subcommands = {
        ClustersCommand.class,
        CreateCommand.class,
        DeleteCommand.class,
        GetCommand.class,
        ListCommand.class,
        PendingChangesCommand.class,
        RepoCommand.class,
        StatusCommand.class,
        UpdateCommand.class
    }
)
public class ProjectCommand extends AbstractCommandGroup {
}
