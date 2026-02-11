package com.streamx.cli.util;

import static com.streamx.cli.util.Output.print;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BannerPrinter {
  private static final String BANNER = """
       ____  _                           __  __
      / ___|| |_ _ __ ___  __ _ _ __ ___ \\ \\/ /
      \\___ \\| __| '__/ _ \\/ _` | '_ ` _ \\ \\  /\s
       ___) | |_| | |  __/ (_| | | | | | |/  \\\s
      |____/ \\__|_|  \\___|\\__,_|_| |_| |_/_/\\_\\.dev
                                               \s""";

  public static void printBanner() {
    print(BANNER);
  }
}
