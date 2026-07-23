package com.streamx.cli.platform;

import java.util.Collections;
import java.util.Iterator;

/**
 * Marker for dynamic invited-email completion. The zsh completion script resolves the values
 * at TAB time via the hidden {@code __complete-invited-emails <orgId>} command, passing the
 * organization ID already typed on the command line.
 */
public class InvitedEmailCompletionCandidates implements Iterable<String> {
  @Override
  public Iterator<String> iterator() {
    return Collections.emptyIterator();
  }
}
