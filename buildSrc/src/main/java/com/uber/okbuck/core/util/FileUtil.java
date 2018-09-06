package com.uber.okbuck.core.util;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.uber.okbuck.template.common.LoadStatements;
import com.uber.okbuck.template.core.Rule;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FileUtil {
  private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

  private static final byte[] NEWLINE = System.lineSeparator().getBytes(StandardCharsets.UTF_8);

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

  public static void writeToBuckFile(List<Rule> rules, File buckFile) {
    writeToBuckFile(TreeMultimap.create(), rules, buckFile);
  }

  @SuppressWarnings("InconsistentOverloads")
  public static void writeToBuckFile(Multimap<String, String> loadStatements, List<Rule> rules, File buckFile) {
    if (!rules.isEmpty()) {
      File parent = buckFile.getParentFile();
      if (!parent.exists() && !parent.mkdirs()) {
        throw new IllegalStateException("Couldn't create dir: " + parent);
      }

      try (
          OutputStream fos = new FileOutputStream(buckFile);
          BufferedOutputStream os = new BufferedOutputStream(fos)){

        if (!loadStatements.isEmpty()) {
          LoadStatements.template(writableLoadStatements(loadStatements)).render(os);
        }

        for (int index = 0; index < rules.size(); index++) {
          // Don't add a new line before the first rule
          if (index != 0) {
            os.write(NEWLINE);
          }
          rules.get(index).render(os);
        }
      } catch (IOException e) {
        throw new IllegalStateException("Couldn't create the buck file", e);
      }
    }
  }

  private static List<String> writableLoadStatements(Multimap<String, String> loadStatements) {
    return loadStatements.asMap().entrySet().stream()
        .map(loadStatement -> Stream
            .concat(Stream.of(loadStatement.getKey()), loadStatement.getValue().stream())
            .map(statement -> "'" + statement + "'")
            .collect(Collectors.joining(", ", "load(", ")")))
        .collect(Collectors.toList());
  }

  public static boolean isZipFile(File file) {
    if (!file.exists() || file.isDirectory() || !file.canRead() || file.length() < 4) {
      return false;
    }
    try (DataInputStream in =
        new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
      return in.readInt() == 0x504b0304;
    } catch (IOException ignored) {
      return false;
    }
  }

  public static void symlink(Path link, Path target) {
    try {
      LOG.info("Creating symlink {} -> {}", link, target);
      Files.createSymbolicLink(link, target);
    } catch (IOException e) {
      LOG.error("Could not create symlink {} -> {}", link, target);
      throw new IllegalStateException(e);
    }
  }
}
