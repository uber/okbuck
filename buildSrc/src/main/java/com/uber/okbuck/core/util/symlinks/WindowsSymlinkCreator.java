package com.uber.okbuck.core.util.symlinks;

import com.uber.okbuck.core.util.windowsfs.WindowsFS;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WindowsSymlinkCreator implements SymlinkCreator {

  private final WindowsFS windowsFS = new WindowsFS();

  @Override
  public void createSymbolicLink(Path symlink, Path target) throws IOException {
    Path normalizedTarget = symlink.getParent().resolve(target).normalize();
    windowsFS.createSymbolicLink(symlink, normalizedTarget, Files.isDirectory(normalizedTarget));
  }
}
