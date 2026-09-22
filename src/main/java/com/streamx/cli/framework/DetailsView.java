package com.streamx.cli.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The text view of a single resource.
 * {@link TextTable} to display details for multiple objects.
 */
public final class DetailsView {

  private static final String ABSENT = "-";

  private final List<String[]> rows = new ArrayList<>();

  public DetailsView row(String label, String value) {
    rows.add(new String[] {label, value == null || value.isBlank() ? ABSENT : value});
    return this;
  }

  public String render() {
    int width = rows.stream().mapToInt(row -> row[0].length()).max().orElse(0);
    String format = "%-" + width + "s = %s";
    return rows.stream()
        .map(row -> format.formatted(row[0], row[1]))
        .collect(Collectors.joining(System.lineSeparator()));
  }
}
