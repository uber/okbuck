package com.uber.okbuck.core.util.symlinks;

public final class SymlinkCreatorFactory {

  private SymlinkCreatorFactory() {}

  public static SymlinkCreator getSymlinkCreator() {
    return new GeneralSymlinkCreator();
  }
}
