package com.uber.okbuck.transform;

import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import java.io.File;

/** Transform runner that instantiates the transform class and starts the transform invocation. */
class TransformRunner {

  private final File inputJarsDir;
  private final File outputJarsDir;
  private final File configFile;
  private final String[] androidClassPath;
  private final Class<Transform> transformClass;

  /**
   * Constructor.
   *
   * @param configFilePath the path to the config file.
   * @param inJarsDir input jars folder.
   * @param outJarsDir output jars folder.
   * @param androidClassPath android classpath.
   * @param transformClass class for the transform.
   */
  TransformRunner(
      String configFilePath,
      String inJarsDir,
      String outJarsDir,
      String[] androidClassPath,
      Class<Transform> transformClass) {
    this.androidClassPath = androidClassPath;
    this.transformClass = transformClass;

    // Check input class path.
    this.inputJarsDir = getFile(inJarsDir, "Input jars dir not existing or invalid.", true);
    System.out.println("Input jars dir: " + inputJarsDir.getAbsolutePath());

    // Check output class path.
    this.outputJarsDir = getFile(outJarsDir, "Output jars dir not existing or invalid.", true);
    System.out.println("Output jars dir: " + outputJarsDir.getAbsolutePath());

    // Reading config file.
    if (configFilePath != null) {
      this.configFile = getFile(configFilePath, "Config file not existing or invalid.", false);
      System.out.println("Config file: " + configFile.getAbsolutePath());
    } else {
      this.configFile = null;
    }
  }

  private static File getFile(String path, String errorMsg, boolean isFolder) {
    File file = new File(path);
    if (!file.exists() || isFolder != file.isDirectory()) {
      throw new IllegalArgumentException(errorMsg);
    }
    return file;
  }

  /**
   * Starts the transform. Here the transform is instantiated through reflection. If the config file
   * is specified, a constructor with a single parameter accepting a {@link File} is used, otherwise
   * an empty constructor.
   *
   * @throws Exception for any exception happened during the transform process.
   */
  void runTransform() throws Exception {
    Transform transform =
        configFile != null
            ? transformClass.getConstructor(File.class).newInstance(configFile)
            : transformClass.newInstance();

    TransformOutputProvider transformOutputProvider =
        new JarsTransformOutputProvider(outputJarsDir, inputJarsDir);

    runTransform(transform, transformOutputProvider);
  }

  private void runTransform(Transform transform, TransformOutputProvider outputProvider)
      throws Exception {

    // Cleaning output directory.
    FileUtil.deleteDirectory(outputJarsDir);
    outputJarsDir.mkdirs();

    // Preparing Transform invocation.
    TransformInput input = new TransformInputBuilder().addJarInputFolder(inputJarsDir).build();
    TransformInput referencedInput =
        new TransformInputBuilder().addJarInput(androidClassPath).build();

    TransformInvocation invocation =
        new TransformInvocationBuilder()
            .addInput(input)
            .addReferencedInput(referencedInput)
            .setOutputProvider(outputProvider)
            .build();

    // Running the transform invocation.
    transform.transform(invocation);
  }
}
