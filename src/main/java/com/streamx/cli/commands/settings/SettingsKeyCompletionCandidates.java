package com.streamx.cli.commands.settings;

import com.streamx.cli.config.StreamxHome;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;

public class SettingsKeyCompletionCandidates implements Iterable<String> {
  @Override
  public Iterator<String> iterator() {
    return loadKeys().iterator();
  }

  public static Iterable<String> loadKeys() {
    try {
      URL url = StreamxHome.getConfigUrl();
      try (InputStream input = url.openStream()) {
        Properties properties = new Properties();
        properties.load(input);
        return new TreeSet<>(properties.stringPropertyNames());
      }
    } catch (Exception ignored) {
      return Collections.emptyList();
    }
  }
}
