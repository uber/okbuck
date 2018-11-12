package com.uber.okbuck.extension;

public class JetifierExtension {

  public static final String DEFAULT_JETIFIER_VERSION = "1.0.0-beta02";

  /** Jetifier jar version */
  public String version;

  JetifierExtension() {
    version = DEFAULT_JETIFIER_VERSION;
  }
}
