package com.streamx.cli.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** A vertical list of titled detail sections, suitable for long values. */
public final class TextList {

  private final List<String> items = new ArrayList<>();

  public TextList item(String title, DetailsView details) {
    String newline = System.lineSeparator();
    String body = details.render().lines()
        .map(line -> "  " + line)
        .collect(Collectors.joining(newline));
    items.add(body.isEmpty() ? title : title + newline + body);
    return this;
  }

  public String render() {
    return String.join(System.lineSeparator().repeat(2), items);
  }
}
