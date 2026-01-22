package com.streamx.cli.framework.testing;

public class UnserializableObject {
  // Object with circular reference to make it unserializable
  public UnserializableObject self = this;
}
