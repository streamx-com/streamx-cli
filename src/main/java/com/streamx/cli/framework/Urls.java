package com.streamx.cli.framework;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Urls {

  private static final Pattern LOOPBACK_IPV4 =
      Pattern.compile("127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

  private Urls() {
  }

  public static boolean isCleartextRemote(String url) {
    String trimmed = url == null ? "" : url.trim();
    if (!trimmed.regionMatches(true, 0, "http://", 0, 7)) {
      return false;
    }
    URI uri;
    try {
      uri = URI.create(trimmed);
    } catch (IllegalArgumentException unparseable) {
      return true;
    }
    String host = uri.getHost();
    if (host == null) {
      return true;
    }
    String normalized = host.toLowerCase(Locale.ROOT);
    return !("localhost".equals(normalized)
        || "::1".equals(normalized)
        || "[::1]".equals(normalized)
        || LOOPBACK_IPV4.matcher(normalized).matches());
  }
}
