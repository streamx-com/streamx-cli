package com.streamx.cli.platform;

import java.util.Iterator;
import java.util.List;

public class Roles implements Iterable<String> {

  public static final String OWNER = "owner";
  public static final String EDIT = "edit";
  public static final String VIEW = "view";

  private static final List<String> ALL = List.of(OWNER, EDIT, VIEW);

  @Override
  public Iterator<String> iterator() {
    return ALL.iterator();
  }
}
