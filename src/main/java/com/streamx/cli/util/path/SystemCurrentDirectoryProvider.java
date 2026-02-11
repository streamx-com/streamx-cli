package com.streamx.cli.util.path;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SystemCurrentDirectoryProvider implements CurrentDirectoryProvider {

  @Override
  public String resolve() {
    return "./";
  }
}
