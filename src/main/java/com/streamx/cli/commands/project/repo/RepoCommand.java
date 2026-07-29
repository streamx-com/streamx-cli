package com.streamx.cli.commands.project.repo;

import com.streamx.cli.commands.project.repo.get.GetCommand;
import com.streamx.cli.commands.project.repo.remove.RemoveCommand;
import com.streamx.cli.commands.project.repo.set.SetCommand;
import com.streamx.cli.commands.project.repo.sshkey.SshKeyCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "repo",
    header = "Manage the Git repository connected to a project",
    subcommands = {
        GetCommand.class,
        SetCommand.class,
        RemoveCommand.class,
        SshKeyCommand.class
    }
)
public class RepoCommand extends AbstractCommandGroup {
}
