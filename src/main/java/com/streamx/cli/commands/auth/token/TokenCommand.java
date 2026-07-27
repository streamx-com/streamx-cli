package com.streamx.cli.commands.auth.token;

import com.streamx.cli.commands.auth.token.create.CreateCommand;
import com.streamx.cli.commands.auth.token.list.ListCommand;
import com.streamx.cli.commands.auth.token.revoke.RevokeCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "token",
    header = "Manage personal access tokens",
    description = {
        "Personal access tokens authenticate the CLI in CI and other non-interactive environments.",
        "Set STREAMX_PLATFORM_TOKEN=<token> to use one for platform calls, no login needed.",
        "A token acts as you, with your permissions, and does not expire until revoked.",
        "These subcommands need a login session: a token cannot manage tokens, so unset "
            + "STREAMX_PLATFORM_TOKEN to use them."
    },
    subcommands = {
        CreateCommand.class,
        ListCommand.class,
        RevokeCommand.class
    }
)
public class TokenCommand extends AbstractCommandGroup {
}
