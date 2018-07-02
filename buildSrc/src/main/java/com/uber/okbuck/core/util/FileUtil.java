package com.uber.okbuck.core.util;

import com.uber.okbuck.template.core.Rule;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;

public final class FileUtil {

  private static final byte[] NEWLINE = System.lineSeparator().getBytes();

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

  public static void writeToBuckFile(List<Rule> rules, File buckFile) {
    if (!rules.isEmpty()) {
      File parent = buckFile.getParentFile();
      if (!parent.exists() && !parent.mkdirs()) {
        throw new IllegalStateException("Couldn't create dir: " + parent);
      }

      try {
        buckFile.createNewFile();

        final OutputStream os = new FileOutputStream(buckFile);

        for (int index = 0; index < rules.size(); index++) {
          // Don't add a new line before the first rule
          if (index != 0) {
            os.write(NEWLINE);
          }
          rules.get(index).render(os);
        }
        os.flush();
        os.close();
      } catch (IOException e) {
        throw new IllegalStateException("Couldn't create the buck file: %s", e);
      }
    }
  }
}
