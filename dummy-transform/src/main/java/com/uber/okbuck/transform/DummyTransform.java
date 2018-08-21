package com.uber.okbuck.transform;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.utils.FileUtils;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class DummyTransform extends Transform {

  private static final String TRANSFORM_NAME = "DummyTransform";
  private static final String TRANSFORM_OUTPUT = "dummy-transform-output";

  @Override
  public String getName() {
    return TRANSFORM_NAME;
  }

  @Override
  public Set<QualifiedContent.ContentType> getInputTypes() {
    return ImmutableSet.<QualifiedContent.ContentType>of(
        QualifiedContent.DefaultContentType.CLASSES);
  }

  @Override
  public Set<QualifiedContent.Scope> getScopes() {
    return ImmutableSet.of(QualifiedContent.Scope.PROJECT, QualifiedContent.Scope.SUB_PROJECTS);
  }

  @Override
  public Set<QualifiedContent.Scope> getReferencedScopes() {
    return ImmutableSet.of(
        QualifiedContent.Scope.SUB_PROJECTS,
        QualifiedContent.Scope.PROJECT_LOCAL_DEPS,
        QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,
        QualifiedContent.Scope.EXTERNAL_LIBRARIES);
  }

  @Override
  public boolean isIncremental() {
    return true;
  }

  @Override
  public void transform(TransformInvocation transformInvocation)
      throws TransformException, InterruptedException, IOException {

    if (transformInvocation.getOutputProvider() == null) {
      throw new IllegalArgumentException("Transform invocation needs a valid output provider.");
    }

    File outputDir =
        transformInvocation
            .getOutputProvider()
            .getContentLocation(TRANSFORM_OUTPUT, getOutputTypes(), getScopes(), Format.DIRECTORY);

    for (TransformInput input : transformInvocation.getInputs()) {

      for (JarInput inputJar : input.getJarInputs()) {
        File outputJar =
            transformInvocation
                .getOutputProvider()
                .getContentLocation(inputJar.getName(), getOutputTypes(), getScopes(), Format.JAR);
        outputJar.getParentFile().mkdirs();

        if (!transformInvocation.isIncremental() || inputJar.getStatus() != Status.REMOVED) {
          FileUtils.copyFile(inputJar.getFile(), outputJar);
        }
      }

      for (DirectoryInput directoryInput : input.getDirectoryInputs()) {

        File inputDir = directoryInput.getFile();
        if (transformInvocation.isIncremental()) {

          for (Map.Entry<File, Status> changedInput : directoryInput.getChangedFiles().entrySet()) {

            File inputFile = changedInput.getKey();
            File outputFile =
                new File(outputDir, FileUtils.relativePossiblyNonExistingPath(inputFile, inputDir));

            switch (changedInput.getValue()) {
              case REMOVED:
                FileUtils.delete(outputFile);
                break;

              case ADDED:
              case CHANGED:
                FileUtils.copyFile(inputFile, outputFile);
            }
          }
        } else {

          FileUtils.deleteDirectoryContents(inputDir);
          inputDir.mkdirs();

          Iterable<File> files = FileUtils.getAllFiles(inputDir);
          for (File inputFile : files) {

            File outputFile = new File(outputDir, FileUtils.relativePath(inputFile, inputDir));
            FileUtils.copyFile(inputFile, outputFile);
          }
        }
      }
    }
  }
}
