package com.uber.okbuck.transform;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.TransformInput;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A builder for {@link TransformInput}. */
class TransformInputBuilder {

  private static final String DOT_JAR = ".jar";

  private final LinkedList<JarInput> jarInputs;
  private final LinkedList<DirectoryInput> directoryInputs;

  /** Constructor. */
  TransformInputBuilder() {
    this.jarInputs = new LinkedList<>();
    this.directoryInputs = new LinkedList<>();
  }

  /**
   * Adds a jar input for this transform input.
   *
   * @param file the file of the jar input.
   * @return this instance of the builder.
   */
  TransformInputBuilder addJarInput(File file) {
    if (file.exists()) {
      this.jarInputs.add(new FileJarInput(file));
      System.out.println("Adding dependency jar: " + file.getAbsolutePath());
    } else {
      System.out.println("Specified jar input doesn't exist: " + file.getAbsolutePath());
    }
    return this;
  }

  /**
   * Adds a jar input folder for this transform input. All the jars found will be added to the class
   * path.
   *
   * @param folder the folder of the jars input.
   * @return this instance of the builder.
   */
  TransformInputBuilder addJarInputFolder(File folder) {
    File[] listFiles = folder.listFiles();
    if (listFiles != null) {
      for (File file : listFiles) {
        if (file.isDirectory()) {
          addJarInputFolder(file);
        } else {
          if (!file.getAbsolutePath().endsWith(DOT_JAR)) {
            continue;
          }
          addJarInput(file);
        }
      }
    }
    return this;
  }

  /**
   * Adds multiple jar inputs for this transform input.
   *
   * @param filePaths the paths of the jars.
   * @return this instance of the builder.
   */
  TransformInputBuilder addJarInput(String... filePaths) {
    return addJarInput(Arrays.asList(filePaths));
  }

  /**
   * Adds multiple jar inputs for this transform input.
   *
   * @param filePaths the paths of the jars.
   * @return this instance of the builder.
   */
  TransformInputBuilder addJarInput(List<String> filePaths) {
    for (String filePath : filePaths) {
      addJarInput(new File(filePath));
    }
    return this;
  }

  /**
   * Adds a directory input for this transform input.
   *
   * @param file the file of the directory input.
   * @return this instance of the builder.
   */
  TransformInputBuilder addDirectoryInput(File file) {
    if (!file.exists()) {
      throw new IllegalArgumentException(
          "Specified directory input doesn't exist: " + file.getAbsolutePath());
    }
    this.directoryInputs.add(new FileDirectoryInput(file));
    return this;
  }

  /**
   * Builds the {@link TransformInput}.
   *
   * @return a new {@link TransformInput} with the specified jar and directories.
   */
  TransformInput build() {
    return new TransformInput() {
      @Override
      public Collection<JarInput> getJarInputs() {
        return jarInputs;
      }

      @Override
      public Collection<DirectoryInput> getDirectoryInputs() {
        return directoryInputs;
      }
    };
  }

  /** A {@link JarInput} with specified file. */
  private static class FileJarInput implements JarInput {

    private final File file;

    FileJarInput(File file) {
      this.file = file;
    }

    @Override
    public File getFile() {
      return file;
    }

    @Override
    public Status getStatus() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
      return file.getAbsolutePath();
    }

    @Override
    public Set<ContentType> getContentTypes() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<Scope> getScopes() {
      throw new UnsupportedOperationException();
    }
  }

  /** A {@link DirectoryInput} with specified file. */
  private static class FileDirectoryInput implements DirectoryInput {

    private final File file;

    FileDirectoryInput(File file) {
      this.file = file;
    }

    @Override
    public File getFile() {
      return file;
    }

    @Override
    public String getName() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<ContentType> getContentTypes() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<Scope> getScopes() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<File, Status> getChangedFiles() {
      throw new UnsupportedOperationException();
    }
  }
}
