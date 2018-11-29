package com.uber.okbuck.core.dependency;

import com.google.common.base.Strings;
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
      @Nullable String version,
      File dependencyFile,
      ExternalDependenciesExtension externalDependenciesExtension,
      JetifierExtension jetifierExtension) {
    String classifier = DependencyUtils.getModuleClassifier(dependencyFile.getName(), version);
    return new ExternalDependency(
        group,
        name,
        Strings.isNullOrEmpty(version) ? LOCAL_DEP_VERSION : version,
        classifier,
        dependencyFile,
        externalDependenciesExtension,
        jetifierExtension);
  }

  /**
   * Create an External Dependency from a local dependency
   *
   * @param localDep local dependency file
   * @param externalDependenciesExtension External Dependency Extension
   * @param jetifierExtension Jetifier Extension
   * @return External Dependency
   */
  public static LocalExternalDependency fromLocal(
      File localDep,
      ExternalDependenciesExtension externalDependenciesExtension,
      JetifierExtension jetifierExtension) {

    return new LocalExternalDependency(
        LOCAL_GROUP,
        FilenameUtils.getBaseName(localDep.getName()),
        LOCAL_DEP_VERSION,
        null,
        localDep,
        externalDependenciesExtension,
        jetifierExtension);
  }
}
