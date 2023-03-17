package com.uber.okbuck.core.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.uber.okbuck.core.util.symlinks.SymlinkCreator;
import com.uber.okbuck.core.util.symlinks.SymlinkCreatorFactory;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileUtil {
  private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);
  private static final SymlinkCreator symlinkCreator = SymlinkCreatorFactory.getSymlinkCreator();

  private static final String DS_STORE = ".DS_Store";

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

  public static String readString(File f) {
    try {
      return new String(Files.readAllBytes(f.toPath()), UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read from file " + f + " due to exception: " + e);
    }
  }

  public static void writeString(File f, String content) {
    try {
      Files.write(f.toPath(), content.getBytes(UTF_8));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write to file " + f + " due to exception: " + e);
    }
  }

  public static void copyResourceToProject(String resource, File destination) {
    try {
      FileUtils.copyURLToFile(FileUtil.class.getResource(resource), destination);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static ImmutableSet<String> available(Project project, Collection<File> files) {
    return files
        .stream()
        .filter(
            rootFile -> {
              if (rootFile.isDirectory() && rootFile.exists()) {
                // Check if the directory contains any valid file
                try (Stream<Path> fileTree = Files.walk(rootFile.toPath())) {
                  return fileTree
                      .filter(
                          currentFilePath -> {
                            File currentFile = currentFilePath.toFile();
                            return !currentFile.isDirectory()
                                && !currentFile.getName().equals(DS_STORE);
                          })
                      .iterator()
                      .hasNext();
                } catch (IOException e) {
                  throw new IllegalStateException(e);
                }
              }
              return rootFile.exists();
            })
        .map(f -> getRelativePath(project.getProjectDir(), f))
        .collect(MoreCollectors.toImmutableSet());
  }

  @SuppressWarnings("EmptyCatch")
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

  public static void symlink(Path link, Path target) {
    try {
      LOG.info("Creating symlink {} -> {}", link, target);
      symlinkCreator.createSymbolicLink(link, target);
    } catch (IOException e) {
      LOG.error("Could not create symlink {} -> {}", link, target);
      throw new IllegalStateException(e);
    }
  }

  public static HashMap<String, String> readMapFromJsonFile(File file) throws IOException {
    Reader fileReader = Files.newBufferedReader(file.toPath(), UTF_8);
    Gson gson = new Gson();
    return gson.fromJson(fileReader, new TypeToken<HashMap<String, String>>() {}.getType());
  }

  public static void persistMapToJsonFile(HashMap<String, String> map, File file)
      throws IOException {
    Writer writer = Files.newBufferedWriter(file.toPath(), UTF_8);
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    gson.toJson(map, writer);
    writer.flush();
    writer.close();
  }

  public static void deleteQuitelyAndCreate(File dir, boolean deleteDir, String fileName) {

    // Clean the dir, if it exists, based on deleteDir config
    if (dir.exists()) {
      if (deleteDir) {
        try {
          FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
          throw new IllegalStateException("Could not delete dependency directory: " + dir, e);
        }
      } else {
        FileUtils.listFiles(dir, new NameFileFilter(fileName), null)
            .parallelStream()
            .forEach(FileUtils::deleteQuietly);
      }
    }

    if (!dir.exists() && !dir.mkdirs()) {
      throw new IllegalStateException("Couldn't create dependency directory: " + dir);
    }
  }
}
