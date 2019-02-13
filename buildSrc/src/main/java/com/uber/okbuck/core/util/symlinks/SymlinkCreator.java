package com.uber.okbuck.core.util.symlinks;

import java.io.IOException;
import java.nio.file.Path;

public interface SymlinkCreator {
  void createSymbolicLink(Path symlink, Path target) throws IOException;
}
