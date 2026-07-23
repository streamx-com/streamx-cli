package com.streamx.cli.platform;

import java.util.Collections;
import java.util.Iterator;

/**
 * Marker for dynamic project-ID completion. The zsh completion script resolves the values at
 * TAB time via the hidden {@code __complete-project-ids <orgId>} command, passing the
 * organization ID already typed on the command line.
 */
public class ProjectIdCompletionCandidates implements Iterable<String> {
  @Override
  public Iterator<String> iterator() {
    return Collections.emptyIterator();
  }
}
