package com.uber.okbuck.transform;

import com.android.build.api.transform.Format;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.TransformOutputProvider;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;

/** A {@link TransformOutputProvider} providing jar outputs based on a given output folder. */
public class JarsTransformOutputProvider implements TransformOutputProvider {

  private final File outputFolder;
  private final File inputFolder;
  private final String[] outputFolderParts;

  /** Constructor. */
  JarsTransformOutputProvider(File outputFolder, File inputFolder) {
    this.outputFolder = outputFolder;
    this.inputFolder = inputFolder;
    this.outputFolderParts = outputFolder.getAbsolutePath().split(File.separator);
  }

  @Override
  public void deleteAll() throws IOException {
    FileUtil.deleteDirectory(outputFolder);
    outputFolder.mkdirs();
  }

  @Override
  public File getContentLocation(
      String name,
      Set<QualifiedContent.ContentType> types,
      Set<? super QualifiedContent.Scope> scopes,
      Format format) {

    // Just a temp directory not to be used, to make the transform happy.
    if (format == Format.DIRECTORY) {
      return new File(System.getProperty("java.io.tmpdir"));
    }

    /**
     * For jars, the name is actually the absolute path. The goal here is to calculate the absolute
     * output path starting from the input one. Since The output path will be different only in the
     * last folder (replaced from the base output folder), it's possible to exclude from the input
     * path the parts in common plus the last folder (i.e. all the parts of the output folder).
     *
     * <p>Example of input path: ...mobile/okbuck/buck-out/bin/app
     * /java_classes_preprocess_in_bin_prodDebug/buck-out/gen/.okbuck/workspace/__app.rxscreenshotdetector-release
     * .aar#aar_prebuilt_jar__/classes.jar
     *
     * <p>Example of output base folder: ...mobile/okbuck/buck-out/bin/app/
     * java_classes_preprocess_out_bin_prodDebug/
     *
     * <p>Example of output path: ...mobile/okbuck/buck-out/bin/app
     * /java_classes_preprocess_out_bin_prodDebug/buck-out/gen/.okbuck/workspace/__app.rxscreenshotdetector-release
     * .aar#aar_prebuilt_jar__/classes.jar
     */

    // If the full absolute path was in name.
    final File file;
    if (name.startsWith(inputFolder.getAbsolutePath())) {
      String[] nameParts = name.split(File.separator);
      LinkedList<String> baseFolderParts = new LinkedList(Arrays.asList(nameParts));
      for (int i = 0; i < outputFolderParts.length; i++) {
        baseFolderParts.removeFirst();
      }
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < baseFolderParts.size(); i++) {
        sb.append(baseFolderParts.get(i));
        if (i != baseFolderParts.size() - 1) {
          sb.append(File.separator);
        }
      }
      file = new File(outputFolder, sb.toString());
    }
    // If just the filename was passed.
    else {
      if (!name.endsWith(".jar")) {
        name += ".jar";
      }
      file = new File(outputFolder, name);
    }
    return file;
  }
}
