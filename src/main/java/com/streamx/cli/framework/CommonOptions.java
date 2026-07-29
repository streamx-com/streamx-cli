package com.streamx.cli.framework;

import com.streamx.cli.config.ContextNameCompletionCandidates;
import picocli.CommandLine;

public class CommonOptions {
  public static final String VERBOSE_SHORT = "-v";
  public static final String VERBOSE_LONG = "--verbose";

  public static final String OUTPUT_SHORT = "-o";
  public static final String OUTPUT_LONG = "--output";

  public static final String STREAMX_HOME_SHORT = "-H";
  public static final String STREAMX_HOME_LONG = "--streamx-home";

  public static final String CONTEXT_SHORT = "-C";
  public static final String CONTEXT_LONG = "--context";

  public static final String HELP_SHORT = "-h";
  public static final String HELP_LONG = "--help";

  public static final String VERSION_SHORT = "-V";
  public static final String VERSION_LONG = "--version";

  @CommandLine.Option(
      names = {HELP_SHORT, HELP_LONG},
      usageHelp = true,
      description = "Show this help message and exit"
  )
  boolean help;

  @CommandLine.Option(
      names = {STREAMX_HOME_SHORT, STREAMX_HOME_LONG},
      description = "StreamX home path [default: ~/.streamx, env: STREAMX_HOME]"
  )
  public String streamxHome;

  @CommandLine.Option(
      names = {CONTEXT_SHORT, CONTEXT_LONG},
      description = "Context to use for this invocation [default: default, env: STREAMX_CONTEXT]",
      completionCandidates = ContextNameCompletionCandidates.class
  )
  public String context;

  @CommandLine.Option(
      names = {VERSION_SHORT, VERSION_LONG},
      versionHelp = true,
      description = "Print version information and exit"
  )
  boolean version;
}
