package dev.streamx.cli.framework.cli.testing;

public class UnserializableObject {
  // Object with circular reference to make it unserializable
  public UnserializableObject self = this;
}
