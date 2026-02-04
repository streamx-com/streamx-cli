package com.streamx.cli.util;

import static com.streamx.cli.i18n.MessageProvider.msg;

import picocli.CommandLine.IVersionProvider;

public class VersionProvider implements IVersionProvider {

  @Override
  public String[] getVersion() {
    var streamxVersion = StreamxMavenPropertiesUtils.getStreamxCliVersion();
    if (streamxVersion == null) {
      return new String[]{msg.noVersionInformationIncluded()};
    }
    return new String[]{streamxVersion};
  }
}
