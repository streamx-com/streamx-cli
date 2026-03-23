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
      names = {"-V", "--version"},
      versionHelp = true,
      description = "Print version information and exit"
  )
  boolean version;
}
