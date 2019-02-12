package com.uber.okbuck.core.util.symlinks;

import org.apache.commons.lang3.SystemUtils;

public final class SymlinkCreatorFactory {

  private SymlinkCreatorFactory() {}

  public static SymlinkCreator getSymlinkCreator() {
    if (SystemUtils.IS_OS_WINDOWS) {
      return new WindowsSymlinkCreator();
    } else {
      return new GeneralSymlinkCreator();
    }
  }
}
