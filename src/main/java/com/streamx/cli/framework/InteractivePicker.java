package com.streamx.cli.framework;

import static com.streamx.cli.i18n.MessageProvider.msg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public final class InteractivePicker {

  private InteractivePicker() {
  }

  public static BufferedReader stdinReader() {
    return new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
  }

  public interface Session extends AutoCloseable {

    String pick(String prompt, List<String> options);

    @Override
    void close();
  }

  public static Session open() {
    if (System.console() != null) {
      try {
        return new JlineSession();
      } catch (IOException e) {

        return new PlainSession();
      }
    }
    return new PlainSession();
  }

  public static String pick(String prompt, List<String> options) {
    try (Session session = open()) {
      return session.pick(prompt, options);
    }
  }

  public static String pick(BufferedReader reader, String prompt, List<String> options) {
    return plainPick(reader, prompt, options);
  }

  private static String plainPick(BufferedReader reader, String prompt, List<String> options) {
    if (options != null && !options.isEmpty()) {
      for (int i = 0; i < options.size(); i++) {
        System.err.printf("  %d) %s%n", i + 1, options.get(i));
      }
    }
    System.err.print(formatPrompt(prompt, options));
    System.err.flush();

    String line;
    try {
      line = reader.readLine();
    } catch (IOException e) {
      throw new CliException(msg.failedToHandleInteractiveInput(), e);
    }
    if (line == null) {
      return null;
    }
    line = line.strip();
    if (line.isEmpty()) {
      return null;
    }

    if (options != null && !options.isEmpty()) {
      try {
        int idx = Integer.parseInt(line);
        if (idx >= 1 && idx <= options.size()) {
          return options.get(idx - 1);
        }
      } catch (NumberFormatException expected) {
      }
    }

    return line;
  }

  private static final class PlainSession implements Session {
    private final BufferedReader reader = stdinReader();

    @Override
    public String pick(String prompt, List<String> options) {
      return plainPick(reader, prompt, options);
    }

    @Override
    public void close() {

    }
  }

  private static final class JlineSession implements Session {
    private final Terminal terminal;

    JlineSession() throws IOException {
      this.terminal = TerminalBuilder.builder().system(true).build();
    }

    @Override
    public String pick(String prompt, List<String> options) {
      LineReaderBuilder builder = LineReaderBuilder.builder().terminal(terminal);
      if (options != null && !options.isEmpty()) {
        builder.completer(new StringsCompleter(options));
      }
      LineReader reader = builder.build();
      reader.setOpt(LineReader.Option.AUTO_MENU);
      reader.setOpt(LineReader.Option.AUTO_LIST);
      reader.setOpt(LineReader.Option.LIST_AMBIGUOUS);
      try {
        String line = reader.readLine(formatPrompt(prompt, options));
        return line == null ? null : line.strip();
      } catch (EndOfFileException | UserInterruptException e) {
        return null;
      }
    }

    @Override
    public void close() {
      try {
        terminal.close();
      } catch (IOException expected) {
      }
    }
  }

  private static String formatPrompt(String prompt, List<String> options) {
    String base = prompt;
    while (base.endsWith(" ") || base.endsWith(":")) {
      base = base.substring(0, base.length() - 1);
    }
    if (options == null || options.isEmpty()) {
      return base + ": ";
    }
    return base + " " + msg.interactivePickerHint() + ": ";
  }
}
