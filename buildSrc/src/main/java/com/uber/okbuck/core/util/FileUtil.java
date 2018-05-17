package com.uber.okbuck.core.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Set;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.gradle.api.Project;

public final class FileUtil {

  private FileUtil() {}

  public static String getRelativePath(File root, File f) {
    Path fPath = f.toPath().toAbsolutePath();
    Path rootPath = root.toPath().toAbsolutePath();
    if (fPath.startsWith(rootPath)) {
      return fPath.toString().substring(rootPath.toString().length() + 1);
    } else {
      throw new IllegalStateException(fPath + " must be located inside " + rootPath);
    }
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public static void copyResourceToProject(String resource, File destination) {
    destination.getParentFile().mkdirs();
    try (Source a = Okio.source(FileUtil.class.getResourceAsStream(resource));
        BufferedSink b = Okio.buffer(Okio.sink(destination))) {
      b.writeAll(a);
      b.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Set<String> available(Project project, Collection<File> files) {
    return files
        .stream()
        .filter(File::exists)
        .map(f -> getRelativePath(project.getProjectDir(), f))
        .collect(MoreCollectors.toImmutableSet());
  }

  public static void deleteQuietly(Path p) {
    try {
      Files.walkFileTree(
          p,
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
              Files.delete(file);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
              Files.delete(dir);
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException ignored) {
    }
  }
}
