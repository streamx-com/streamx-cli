package com.streamx.cli.commands.settings.eventtemplates;

import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import java.util.Iterator;

public class TemplateIdCompletionCandidates implements Iterable<String> {
  @Override
  public Iterator<String> iterator() {
    return EventTemplateCatalog.templateIds().iterator();
  }
}
