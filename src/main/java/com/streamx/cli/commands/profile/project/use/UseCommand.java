package com.streamx.cli.commands.profile.project.use;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.platform.ContextProjectIdCompletionCandidates;
import com.streamx.cli.platform.PlatformContext;
import picocli.CommandLine;

@CommandLine.Command(
    name = "use",
    header = "Set the current project for the active profile",
    description = "Commands taking a project fall back to it when the argument is omitted. "
        + "Requires a current organization (the project is resolved inside it); "
        + "STREAMX_PROJECT overrides the stored value for a single invocation."
)
public class UseCommand extends AbstractSilentCommand {

  @CommandLine.Parameters(
      index = "0",
      description = "Project ID",
      completionCandidates = ContextProjectIdCompletionCandidates.class
  )
  public String projectId;

  @Override
  public CommandResult<Void> runCommand() {
    PlatformContext.setCurrentProject(projectId.strip());
    System.out.println(msg.projectUseSet(projectId.strip()));
    return new CommandResult<>(null);
  }
}
