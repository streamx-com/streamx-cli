package com.streamx.cli.commands.settings.eventtemplates;

import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import com.streamx.cli.commands.publish.event.EventTemplateCatalog.TemplateLocation;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class NonDefaultTemplateIdCompletionCandidates implements Iterable<String> {
  @Override
  public Iterator<String> iterator() {
    return loadIds().iterator();
  }

  public static List<String> loadIds() {
    try {
      return EventTemplateCatalog.listAll().stream()
          .filter(t -> !EventTemplateCatalog.SOURCE_DEFAULT.equals(t.source()))
          .map(TemplateLocation::id)
          .toList();
    } catch (Exception ignored) {
      return Collections.emptyList();
    }
  }
}
