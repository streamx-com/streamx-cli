package com.streamx.cli.platform;

import java.util.Iterator;
import java.util.List;

public class Roles implements Iterable<String> {

  private static final List<String> ALL = List.of("owner", "edit", "view");

  @Override
  public Iterator<String> iterator() {
    return ALL.iterator();
  }
}
