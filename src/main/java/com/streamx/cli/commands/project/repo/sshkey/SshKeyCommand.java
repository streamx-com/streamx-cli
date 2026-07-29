package com.streamx.cli.commands.project.repo.sshkey;

import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "ssh-key",
    header = "Manage the SSH deploy key of the connected repository",
    subcommands = {
        SetSshKeyCommand.class,
        ShowSshKeyCommand.class,
        RemoveSshKeyCommand.class,
        GenerateSshKeyCommand.class
    }
)
public class SshKeyCommand extends AbstractCommandGroup {
}
