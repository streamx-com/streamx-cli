package com.streamx.cli.framework;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextListTest {

  @Test
  void shouldSeparateItemsAndIndentEveryDetailLineWithoutTruncatingValues() {
    String value = "long-dns-value-".repeat(20);
    String output = new TextList()
        .item("First record", new DetailsView().row("Value", value + "\nsecond line"))
        .item("Second record", new DetailsView().row("Name", null))
        .render();

    assertThat(output).isEqualToNormalizingNewlines("First record\n  Value = " + value
        + "\n  second line\n\nSecond record\n  Name = -");
  }

  @Test
  void shouldRenderAnEmptyListWithoutOutput() {
    assertThat(new TextList().render()).isEmpty();
  }
}
