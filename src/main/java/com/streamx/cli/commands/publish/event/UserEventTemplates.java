package com.streamx.cli.commands.publish.event;

import com.streamx.cli.config.StreamxHome;
import java.nio.file.Path;

public final class UserEventTemplates {

  public static final String DIRECTORY = "event-templates/custom";
  public static final String EXTENSION = ".json";

  private UserEventTemplates() {
  }

  public static Path getDirectory() {
    return StreamxHome.getStreamxHome().resolve(DIRECTORY);
  }

  public static Path resolve(String templateName) {
    return getDirectory().resolve(templateName + EXTENSION);
  }
}
