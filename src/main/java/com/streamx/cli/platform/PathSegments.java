package com.streamx.cli.platform;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class PathSegments {

  private PathSegments() {
  }

  public static String encode(String segment) {
    return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
