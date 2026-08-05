package com.streamx.cli.framework;

import java.util.List;

public final class TextTable {

  private static final String COLUMN_SEPARATOR = "  ";
  private static final String ABSENT = "-";

  private TextTable() {
  }

  public static String render(List<String> headers, List<List<String>> rows) {
    int[] widths = new int[headers.size()];
    for (int column = 0; column < headers.size(); column++) {
      widths[column] = headers.get(column).length();
      for (List<String> row : rows) {
        widths[column] = Math.max(widths[column], cell(row, column).length());
      }
    }

    StringBuilder output = new StringBuilder();
    appendRow(output, headers, widths);
    for (List<String> row : rows) {
      output.append("\n");
      appendRow(output, row, widths);
    }
    return output.toString();
  }

  private static void appendRow(StringBuilder output, List<String> row, int[] widths) {
    for (int column = 0; column < widths.length; column++) {
      String value = cell(row, column);
      boolean last = column == widths.length - 1;
      output.append(last ? value : value + " ".repeat(widths[column] - value.length()));
      if (!last) {
        output.append(COLUMN_SEPARATOR);
      }
    }
  }

  private static String cell(List<String> row, int column) {
    if (column >= row.size() || row.get(column) == null) {
      return ABSENT;
    }
    return row.get(column);
  }
}
