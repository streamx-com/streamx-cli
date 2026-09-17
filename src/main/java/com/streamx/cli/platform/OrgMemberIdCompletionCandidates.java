package com.streamx.cli.platform;

import java.util.Collections;
import java.util.Iterator;

/**
 * Marker for dynamic member-ID completion. The zsh completion script resolves the values at
 * TAB time via the hidden {@code __complete-org-member-ids <orgId>} command, passing the
 * organization ID already typed on the command line. Only ACTIVE members are offered, since
 * the commands using this refuse to act on pending invitees.
 */
public class OrgMemberIdCompletionCandidates implements Iterable<String> {
  @Override
  public Iterator<String> iterator() {
    return Collections.emptyIterator();
  }
}
