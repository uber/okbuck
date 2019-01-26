package com.uber.okbuck.core.dependency;

import com.uber.okbuck.extension.ExternalDependenciesExtension;
import com.uber.okbuck.extension.JetifierExtension;
import java.io.File;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;

public final class DependencyFactory {

  private static final String LOCAL_GROUP = "local";
  private static final String LOCAL_DEP_VERSION = "1.0.0-LOCAL";

  private DependencyFactory() {}

  /**
   * Create an External Dependency
   *
   * @param group group of the dependency
   * @param name name of the dependency
   * @param version version of the dependency
   * @param dependencyFile file of the dependency
   * @param externalDependenciesExtension External Dependency Extension
   * @param jetifierExtension Jetifier Extension
   * @return External Dependency
   */
  public static ExternalDependency from(
      String group,
      String name,
      String version,
      File dependencyFile,
      @Nullable File dependencySourceFile,
      ExternalDependenciesExtension externalDependenciesExtension,
      JetifierExtension jetifierExtension) {
    String classifier = DependencyUtils.getModuleClassifier(dependencyFile.getName(), version);

    if (isLocalDependency(dependencyFile.getAbsolutePath())) {
      return new LocalExternalDependency(
          group,
          name,
          version,
          classifier,
          dependencyFile,
          dependencySourceFile,
          externalDependenciesExtension,
          jetifierExtension);
    }

    return new ExternalDependency(
        group,
        name,
        version,
        classifier,
        dependencyFile,
        dependencySourceFile,
        externalDependenciesExtension,
        jetifierExtension);
  }

  /**
   * Create an External Dependency from a local dependency
   *
   * @param localDependency local dependency file
   * @param externalDependenciesExtension External Dependency Extension
   * @param jetifierExtension Jetifier Extension
   * @return External Dependency
   */
  public static LocalExternalDependency fromLocal(
      File localDependency,
      @Nullable File localSourceDependency,
      ExternalDependenciesExtension externalDependenciesExtension,
      JetifierExtension jetifierExtension) {

    return new LocalExternalDependency(
        LOCAL_GROUP,
        FilenameUtils.getBaseName(localDependency.getName()),
        LOCAL_DEP_VERSION,
        null,
        localDependency,
        localSourceDependency,
        externalDependenciesExtension,
        jetifierExtension);
  }

  /**
   * Returns whether the dependency should be marked local or not. These dependencies can't be
   * downloaded via buck. Snapshot dependencies with version suffixed with `-SNAPSHOT`. Local m2
   * dependencies with version suffixed with `-LOCAL`.
   *
   * <p>Note: we need to pass in the whole dependency file instead of just the version. `-SNAPSHOT`
   * dependencies with specific date and time in version is also not supported by buck and hence to
   * access correctly we need to look at the whole path. eg:
   * com/jakewharton/butterknife/9.0.0-SNAPSHOT/butterknife-9.0.0-20181220.030319-77.jar
   *
   * @param dependencyFilePath Absolute path string of the dependency file
   * @return Whether the dependency should be local or not.
   */
  private static boolean isLocalDependency(String dependencyFilePath) {
    return dependencyFilePath.contains("-SNAPSHOT") || dependencyFilePath.contains("-LOCAL");
  }
}
