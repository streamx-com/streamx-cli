package com.streamx.cli.commands.auth;

import com.streamx.cli.commands.auth.login.LoginCommand;
import com.streamx.cli.commands.auth.logout.LogoutCommand;
import com.streamx.cli.commands.auth.token.TokenCommand;
import com.streamx.cli.commands.auth.whoami.WhoamiCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "auth",
    header = "Manage StreamX authentication",
    subcommands = {
        LoginCommand.class,
        LogoutCommand.class,
        WhoamiCommand.class,
        TokenCommand.class
    }
)
public class AuthCommand extends AbstractCommandGroup {
}
