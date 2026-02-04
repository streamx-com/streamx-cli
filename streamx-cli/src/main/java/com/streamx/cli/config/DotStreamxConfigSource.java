package com.streamx.cli.config;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static com.streamx.cli.util.FileUtils.createIfNotExists;

import com.streamx.cli.framework.CliException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

public class DotStreamxConfigSource {
  public static URL getUrl() throws CliException {
    try {
      String homeDir = System.getProperty("user.home");
      Path pathToDir = Path.of(homeDir + "/.streamx/config");
      Path pathToFile = pathToDir.resolve("application.properties");
      File file = createIfNotExists(pathToDir, pathToFile);

      return file.toURI().toURL();
    } catch (IOException e) {
      throw new CliException(msg.unableToGetSettingsFilePath(), e);
    }
  }
}
