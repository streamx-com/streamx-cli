package com.streamx.cli.platform;

import java.util.Collections;
import java.util.Iterator;

/**
 * Marker for dynamic organization-ID completion. The zsh completion script resolves the
 * values at TAB time via the hidden {@code __complete-org-ids} command; iterating here on
 * purpose yields nothing so that script generation never triggers a network call.
 */
public class OrgIdCompletionCandidates implements Iterable<String> {
  @Override
  public Iterator<String> iterator() {
    return Collections.emptyIterator();
  }
}
