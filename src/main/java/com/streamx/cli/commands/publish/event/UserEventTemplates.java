package com.streamx.cli.commands.publish.event;

import com.streamx.cli.config.StreamxHome;
import java.nio.file.Path;

/** Custom event templates of the active context. */
public final class UserEventTemplates {

  public static final String EXTENSION = ".json";

  private UserEventTemplates() {
  }

  public static Path getDirectory() {
    return StreamxHome.getEventTemplatesDir();
  }

  public static Path resolve(String templateName) {
    return getDirectory().resolve(templateName + EXTENSION);
  }
}
