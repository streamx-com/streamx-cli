package com.streamx.cli.util;

import static com.streamx.cli.i18n.MessageProvider.msg;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jetbrains.annotations.NotNull;

public final class ExceptionUtils {

  private ExceptionUtils() {

  }

  public static RuntimeException sneakyThrow(@NotNull Throwable t) {
    return ExceptionUtils.<RuntimeException>sneakyThrow0(t);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Throwable> T sneakyThrow0(Throwable t) throws T {
    throw (T) t;
  }

  public static String appendLogSuggestion(String originalMessage) {
    String logPath = ConfigProvider.getConfig()
        .getValue("quarkus.log.file.path", String.class);

    return msg.fullLogsCanBeFoundIn(originalMessage, logPath);
  }
}
