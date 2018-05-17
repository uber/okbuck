package com.uber.okbuck.transform;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

final class FileUtil {

  private FileUtil() {}

  static void deleteDirectory(File path) {
    try {
      Files.walk(path.toPath(), FileVisitOption.FOLLOW_LINKS)
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    } catch (IOException ignored) {
    }
  }
}
