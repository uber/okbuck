package com.uber.okbuck.android.lint;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class AndroidLintCli {
  private static final String ARG_LIBRARIES = "--libraries";

  public static void main(String[] args) throws IOException {

    for (int index = 0; index < args.length; index++) {
      if (args[index].equals(ARG_LIBRARIES)) {
        args[index + 1] = getLibraryArguments(args[index + 1]);
        break;
      }
    }
    com.android.tools.lint.Main.main(args);
  }

  private static String getLibraryArguments(String filename) throws IOException {
    File file = new File(filename);

    if (!file.isAbsolute()) {
      throw new RuntimeException(String.format("%s should be absolute", file));
    }

    String librariesString = read(file.toPath()).trim();

    // Input string has trailing :
    if (librariesString.endsWith(":")) {
      librariesString = librariesString.substring(0, librariesString.length() - 1);
    }

    return librariesString;
  }

  private static String read(Path file) throws IOException {
    return new String(readAllBytes(file), UTF_8);
  }
}
