package com.streamx.cli.commands.project.domains;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

class DurationConverterTest {

  private final DurationConverter converter = new DurationConverter();

  @Test
  void durationsAreSecondsMinutesOrHours() {
    assertThat(converter.convert("90s")).isEqualTo(Duration.ofSeconds(90));
    assertThat(converter.convert("10M")).isEqualTo(Duration.ofMinutes(10));
    assertThat(converter.convert(" 1h ")).isEqualTo(Duration.ofHours(1));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "10", "0s", "-5m", "1.5h", "PT1H", "1d", "10 m"})
  void otherDurationsAreRejected(String value) {
    assertThatThrownBy(() -> converter.convert(value))
        .isInstanceOf(CommandLine.TypeConversionException.class);
  }

  @Test
  void durationsAreFormattedTheWayTheyAreTyped() {
    assertThat(DurationConverter.format(Duration.ofHours(2))).isEqualTo("2h");
    assertThat(DurationConverter.format(Duration.ofMinutes(90))).isEqualTo("90m");
    assertThat(DurationConverter.format(Duration.ofSeconds(45))).isEqualTo("45s");
  }
}
