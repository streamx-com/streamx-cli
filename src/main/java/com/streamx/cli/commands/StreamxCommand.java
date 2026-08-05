package com.streamx.cli.commands;

import com.streamx.cli.commands.auth.AuthCommand;
import com.streamx.cli.commands.completion.CompleteClusterIdsCommand;
import com.streamx.cli.commands.completion.CompleteContextNamesCommand;
import com.streamx.cli.commands.completion.CompleteInvitedEmailsCommand;
import com.streamx.cli.commands.completion.CompleteNonDefaultTemplateIdsCommand;
import com.streamx.cli.commands.completion.CompleteOrgIdsCommand;
import com.streamx.cli.commands.completion.CompleteOrgMemberIdsCommand;
import com.streamx.cli.commands.completion.CompleteProjectIdsCommand;
import com.streamx.cli.commands.completion.CompleteRegisteredTemplateIdsCommand;
import com.streamx.cli.commands.completion.CompleteSettingsKeysCommand;
import com.streamx.cli.commands.completion.CompleteSettingsSetKeysCommand;
import com.streamx.cli.commands.completion.CompleteTemplateIdsCommand;
import com.streamx.cli.commands.completion.CompleteTokenIdsCommand;
import com.streamx.cli.commands.completion.CompletionCommand;
import com.streamx.cli.commands.context.ContextCommand;
import com.streamx.cli.commands.info.InfoCommand;
import com.streamx.cli.commands.local.LocalCommand;
import com.streamx.cli.commands.org.OrgCommand;
import com.streamx.cli.commands.project.ProjectCommand;
import com.streamx.cli.commands.publish.PublishCommand;
import com.streamx.cli.commands.settings.SettingsCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import picocli.CommandLine;

@CommandLine.Command(
    name = "streamx",
    header = "StreamX CLI. More info at https://streamx.com",
    subcommands = {
        AuthCommand.class,
        ContextCommand.class,
        OrgCommand.class,
        ProjectCommand.class,
        LocalCommand.class,
        SettingsCommand.class,
        PublishCommand.class,
        InfoCommand.class,
        CompletionCommand.class,
        CompleteTemplateIdsCommand.class,
        CompleteRegisteredTemplateIdsCommand.class,
        CompleteNonDefaultTemplateIdsCommand.class,
        CompleteSettingsKeysCommand.class,
        CompleteSettingsSetKeysCommand.class,
        CompleteContextNamesCommand.class,
        CompleteOrgIdsCommand.class,
        CompleteProjectIdsCommand.class,
        CompleteOrgMemberIdsCommand.class,
        CompleteInvitedEmailsCommand.class,
        CompleteClusterIdsCommand.class,
        CompleteTokenIdsCommand.class
    }
)
public class StreamxCommand extends AbstractCommandGroup {
}
