package com.streamx.cli.commands.settings.eventtemplates;

import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import java.util.Collections;
import java.util.Iterator;

public class RegisteredTemplateIdCompletionCandidates implements Iterable<String> {
  @Override
  public Iterator<String> iterator() {
    return loadIds().iterator();
  }

  public static Iterable<String> loadIds() {
    try {
      return EventTemplateCatalog.listSettingsRegistrations().keySet();
    } catch (Exception ignored) {
      return Collections.emptyList();
    }
  }
}
