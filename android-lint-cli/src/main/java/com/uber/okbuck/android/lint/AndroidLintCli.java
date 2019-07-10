package com.uber.okbuck.android.lint;

import com.android.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;

public class AndroidLintCli {

  private static final String PROP_WORK_DIR = "com.android.tools.lint.workdir";
  private static final String ARG_LIBRARIES = "--libraries";

  public static void main(String[] args) {

    for (int index = 0; index < args.length; index++) {
      if (args[index].equals(ARG_LIBRARIES)) {
        File libraryFile = getLibrariesArgumentPath(args[index + 1]);
        args[index + 1] = read(libraryFile.toPath());
        break;
      }
    }
    com.android.tools.lint.Main.main(args);
  }

  private static File getLibrariesArgumentPath(String filename) {
    File file = new File(filename);

    if (!file.isAbsolute()) {
      File workDir = getLintWorkDir();
      if (workDir != null) {
        File file2 = new File(workDir, filename);
        try {
          file = file2.getCanonicalFile();
        } catch (IOException e) {
          file = file2;
        }
      }
    }
    return file;
  }

  @Nullable
  private static File getLintWorkDir() {
    // First check the Java properties (e.g. set using "java -jar ... -Dname=value")
    String path = System.getProperty(PROP_WORK_DIR);
    if (path == null || path.isEmpty()) {
      // If not found, check environment variables.
      path = System.getenv(PROP_WORK_DIR);
    }
    if (path != null && !path.isEmpty()) {
      return new File(path);
    }
    return null;
  }

  private static String read(Path file) {
    try {
      return new String(readAllBytes(file), UTF_8);
    } catch (Exception ignored) {
      // If can't read file return empty string
    }
    return "";
  }
}
