package com.streamx.cli.config;

import java.util.Iterator;

public class ProfileNameCompletionCandidates implements Iterable<String> {
  @Override
  public Iterator<String> iterator() {
    return StreamxHome.listProfileNames().iterator();
  }
}
