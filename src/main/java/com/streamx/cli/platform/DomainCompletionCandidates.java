package com.streamx.cli.platform;

import java.util.Collections;
import java.util.Iterator;

/** Marker for domain IDs and names resolved by the shell at completion time. */
public class DomainCompletionCandidates implements Iterable<String> {
  @Override
  public Iterator<String> iterator() {
    return Collections.emptyIterator();
  }
}
