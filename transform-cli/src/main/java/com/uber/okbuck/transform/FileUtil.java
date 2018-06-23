package com.uber.okbuck.transform;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

final class FileUtil {

  private FileUtil() {}

  static void deleteDirectory(File path) {
    try (Stream<Path> walk = Files.walk(path.toPath(), FileVisitOption.FOLLOW_LINKS)) {
      walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    } catch (IOException ignored) {
    }
  }
}
