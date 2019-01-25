package com.uber.okbuck.extension;

public class IntellijExtension {

  /** Enable fetching source jars. */
  private boolean sources = false;

  public boolean downloadSources() {
    return sources;
  }
}
