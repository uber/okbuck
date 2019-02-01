package com.uber.okbuck.core.util;

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;

import com.google.errorprone.annotations.Var;
import com.uber.okbuck.core.util.windowsfs.WindowsFS;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileUtil {
  private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

  private FileUtil() {}

  public static String getRelativePath(File root, File f) {
    Path fPath = f.toPath().toAbsolutePath();
    Path rootPath = root.toPath().toAbsolutePath();

    if (fPath.startsWith(rootPath)) {
      return rootPath.relativize(fPath).toString();
    } else {
      throw new IllegalStateException(fPath + " must be located inside " + rootPath);
    }
  }

  public static void copyResourceToProject(String resource, File destination) {
    try {
      FileUtils.copyURLToFile(FileUtil.class.getResource(resource), destination);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static ImmutableSet<String> available(Project project, Collection<File> files) {
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

  public static boolean isZipFile(File file) {
    if (!file.exists() || file.isDirectory() || !file.canRead() || file.length() < 4) {
      return false;
    }

    try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
      return raf.readInt() == 0x504b0304;
    } catch (IOException e) {
      return false;
    }
  }

  public static void symlink(Path link, @Var Path target) {
    try {
      LOG.info("Creating symlink {} -> {}", link, target);
      if (System.getProperty("os.name").startsWith("Windows")) {
        target = link.getParent().resolve(target).normalize();
        WindowsFS windowsFS = new WindowsFS();
        windowsFS.createSymbolicLink(link, target, Files.isDirectory(target));
      } else {
        Files.createSymbolicLink(link, target);
      }
    } catch (IOException e) {
      LOG.error("Could not create symlink {} -> {}", link, target);
      throw new IllegalStateException(e);
    }
  }
}
