package com.streamx.cli.commands.docs;

import com.streamx.cli.docs.MarkdownDocsGenerator;
import com.streamx.cli.framework.AbstractCommand;
import com.streamx.cli.framework.CommandResult;
import com.streamx.cli.framework.CommonOptions;
import java.nio.file.Path;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

@CommandLine.Command(
    name = "__generate-docs",
    hidden = true,
    header = "Internal: write the command reference as Docusaurus markdown"
)
public class GenerateDocsCommand extends AbstractCommand<String> {

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "<output-dir>",
      description = "Directory to write the markdown tree into"
  )
  public Path outputDir;

  @Override
  public List<String> getHiddenOptions() {
    return List.of(CommonOptions.OUTPUT_LONG, CommonOptions.VERBOSE_LONG);
  }


  @Override
  public String getTextOutput(CommandResult<String> result) {
    return result.getData();
  }

  @Override
  public CommandResult<String> runCommand() {
    CommandSpec root = spec;
    while (root.parent() != null) {
      root = root.parent();
    }
    int pages = MarkdownDocsGenerator.generate(root.commandLine(), outputDir);
    return new CommandResult<>("Wrote " + pages + " pages to " + outputDir);
  }
}
