package com.streamx.cli.commands.project.domains;

import static com.streamx.cli.i18n.MessageProvider.msg;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import picocli.CommandLine;

/** Whole seconds, minutes or hours, such as 90s, 10m or 1h. */
@RegisterForReflection
public class DurationConverter implements CommandLine.ITypeConverter<Duration> {

  private static final Pattern DURATION = Pattern.compile("(\\d{1,9})([smh])");

  @Override
  public Duration convert(String value) {
    Matcher matcher = DURATION.matcher(value.trim().toLowerCase(Locale.ROOT));
    if (!matcher.matches() || Long.parseLong(matcher.group(1)) == 0) {
      throw new CommandLine.TypeConversionException(msg.invalidDuration(value));
    }
    long amount = Long.parseLong(matcher.group(1));
    return switch (matcher.group(2)) {
      case "s" -> Duration.ofSeconds(amount);
      case "m" -> Duration.ofMinutes(amount);
      default -> Duration.ofHours(amount);
    };
  }

  /** Formats a duration the way it is typed. */
  public static String format(Duration duration) {
    long seconds = duration.toSeconds();
    if (seconds % 3600 == 0) {
      return seconds / 3600 + "h";
    }
    return seconds % 60 == 0 ? seconds / 60 + "m" : seconds + "s";
  }
}
