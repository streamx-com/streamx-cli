package com.streamx.cli.config;

import java.util.Iterator;

public class ContextNameCompletionCandidates implements Iterable<String> {
  @Override
  public Iterator<String> iterator() {
    return StreamxHome.listContextNames().iterator();
  }
}
