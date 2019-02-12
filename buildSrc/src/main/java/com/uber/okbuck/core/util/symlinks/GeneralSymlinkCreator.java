package com.uber.okbuck.core.util.symlinks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GeneralSymlinkCreator implements SymlinkCreator {

  @Override
  public void createSymbolicLink(Path symlink, Path target) throws IOException {
    Files.createSymbolicLink(symlink, target);
  }
}
