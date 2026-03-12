package com.streamx.cli.util;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.CliException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class StreamxMavenPropertiesUtils {

  private static final Properties PROPERTIES;

  static {
    URL url = StreamxMavenPropertiesUtils.class.getResource("/streamx-maven.properties");
    Properties properties = null;
    if (url != null) {
      try {
        Properties loadedProperties = new Properties();
        loadedProperties.load(url.openStream());

        properties = loadedProperties;
      } catch (IOException e) {
        throw new CliException(msg.failedToInitializeStreamxMavenProperties(), e);
      }
    }

    PROPERTIES = properties;
  }

  public static String getStreamxCliVersion() {
    return getProperty("streamx.cli.version");
  }

  private static String getProperty(String key) {
    return PROPERTIES.getProperty(key);
  }

}
