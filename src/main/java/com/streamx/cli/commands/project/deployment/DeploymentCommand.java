package com.streamx.cli.commands.project.deployment;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamx.cli.commands.project.ProjectScopedOptions;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.AbstractCommandGroup;
import com.streamx.cli.framework.AbstractSilentCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.TextTable;
import com.streamx.cli.platform.PlatformClients;
import com.streamx.cli.platform.PlatformContext;
import com.streamx.cli.platform.ProjectDeploymentApi;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "deployment",
    header = "Inspect and publish project deployment configuration",
    subcommands = {
        DeploymentCommand.Live.class,
        DeploymentCommand.Desired.class,
        DeploymentCommand.Diff.class,
        DeploymentCommand.Discard.class,
        DeploymentCommand.Push.class
    }
)
public class DeploymentCommand extends AbstractCommandGroup {

  public abstract static class ConfigurationCommand extends AbstractCommand<JsonNode> {

    @CommandLine.Mixin
    ProjectScopedOptions scope;

    @Override
    public CommandResult<JsonNode> runCommand() {
      PlatformContext.OrgProject context =
          PlatformContext.orgAndProject(scope.orgId, scope.projectId);
      try (PlatformClients clients = PlatformClients.fromConfig()) {
        return new CommandResult<>(request(new ProjectDeploymentApi(clients), context));
      }
    }

    abstract JsonNode request(ProjectDeploymentApi api, PlatformContext.OrgProject context);
  }

  @CommandLine.Command(name = "live", header = "Show the applied deployment configuration")
  public static class Live extends ConfigurationCommand {

    @Override
    JsonNode request(ProjectDeploymentApi api, PlatformContext.OrgProject context) {
      return api.live(context.org(), context.project());
    }
  }

  @CommandLine.Command(name = "desired", header = "Show the desired deployment configuration")
  public static class Desired extends ConfigurationCommand {

    @Override
    JsonNode request(ProjectDeploymentApi api, PlatformContext.OrgProject context) {
      return api.desired(context.org(), context.project());
    }
  }

  @CommandLine.Command(
      name = "diff",
      header = "Show local changes against the desired configuration"
  )
  public static class Diff extends ConfigurationCommand {

    @CommandLine.Option(names = "--path", paramLabel = "<pointer>",
        description = "Filter changes by JSON Pointer")
    String path;

    @Override
    JsonNode request(ProjectDeploymentApi api, PlatformContext.OrgProject context) {
      return api.diff(context.org(), context.project(), path);
    }

    @Override
    public String getTextOutput(CommandResult<JsonNode> result) {
      JsonNode diff = result.getData();
      if (!diff.path("present").asBoolean()) {
        return msg.projectDeploymentNoLocalChanges();
      }
      List<List<String>> rows = new ArrayList<>();
      for (JsonNode change : diff.path("differences")) {
        rows.add(List.of(
            change.path("operation").asText(),
            change.path("path").asText(),
            change.path("from").asText(""),
            change.path("before").toString(),
            change.path("after").toString()));
      }
      if (rows.isEmpty()) {
        return msg.projectDeploymentEmptyDiff();
      }
      return TextTable.render(List.of("OPERATION", "PATH", "FROM", "BEFORE", "AFTER"), rows);
    }
  }

  @CommandLine.Command(name = "discard", header = "Discard all local deployment changes")
  public static class Discard extends AbstractSilentCommand {

    @CommandLine.Mixin
    ProjectScopedOptions scope;

    @Override
    public CommandResult<Void> runCommand() {
      PlatformContext.OrgProject context =
          PlatformContext.orgAndProject(scope.orgId, scope.projectId);
      try (PlatformClients clients = PlatformClients.fromConfig()) {
        new ProjectDeploymentApi(clients).discard(context.org(), context.project());
      }
      return new CommandResult<>(null);
    }
  }

  @CommandLine.Command(name = "push", header = "Publish the desired deployment configuration")
  public static class Push extends ConfigurationCommand {

    @Override
    JsonNode request(ProjectDeploymentApi api, PlatformContext.OrgProject context) {
      return api.push(context.org(), context.project());
    }

    @Override
    public String getTextOutput(CommandResult<JsonNode> result) {
      JsonNode config = result.getData();
      if (config == null) {
        return msg.projectDeploymentNothingToPush();
      }
      return msg.projectDeploymentPushed(config.path("generation").asLong());
    }
  }
}
