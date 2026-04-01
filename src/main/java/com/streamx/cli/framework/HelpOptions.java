package com.streamx.cli.framework;

import picocli.CommandLine;

public class HelpOptions {
  @CommandLine.Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "Show this help message and exit"
  )
  boolean help;

  @CommandLine.Option(
      names = {CommonOption.STREAMX_HOME_SHORT, CommonOption.STREAMX_HOME_LONG},
      description = "StreamX home path [default: ~/.streamx, env: STREAMX_HOME]"
  )
  public String streamxHome;

  @CommandLine.Option(
      names = {"-V", "--version"},
      versionHelp = true,
      description = "Print version information and exit"
  )
  boolean version;
}
