package com.uber.okbuck.core.dependency;

import static com.uber.okbuck.core.dependency.BaseExternalDependency.AAR;
import static com.uber.okbuck.core.dependency.BaseExternalDependency.JAR;

import com.google.common.collect.ImmutableList;
import com.uber.okbuck.extension.ExternalDependenciesExtension;
import com.uber.okbuck.extension.JetifierExtension;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import org.gradle.api.Project;

public final class LocalExternalDependency extends ExternalDependency {

  /** Returns the cached file name of the sources jar file. */
  public String getSourceFileName() {
    return getSourceFileNameFrom(getDependencyFileName());
  }

  /** Returns the cached file name of the artifact of the dependency. */
  public String getDependencyFileName() {
    return getTargetName();
  }

  @Nullable
  @Override
  Path computeSourceFile(Project project) {
    if (!DependencyUtils.isWhiteListed(getRealDependencyFile())
        && ImmutableList.of(JAR, AAR).contains(getPackaging())) {

      String sourceFileName = getSourceFileNameFrom(getRealDependencyFile().getName());
      Path sourcesJar = getRealDependencyFile().getParentFile().toPath().resolve(sourceFileName);

      if (Files.exists(sourcesJar)) {
        return sourcesJar;
      }
    }
    return null;
  }

  LocalExternalDependency(
      String group,
      String name,
      String version,
      @Nullable String classifier,
      File depFile,
      ExternalDependenciesExtension externalDependenciesExtension,
      JetifierExtension jetifierExtension) {
    super(
        group,
        name,
        version,
        classifier,
        depFile,
        externalDependenciesExtension,
        jetifierExtension);
  }
}
