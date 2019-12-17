package com.uber.okbuck.core.dependency;

import com.google.common.base.Strings;
import com.uber.okbuck.extension.ExternalDependenciesExtension;
import com.uber.okbuck.extension.JetifierExtension;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.gradle.api.artifacts.ExternalDependency;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;

public final class DependencyFactory {

  public static final String LOCAL_GROUP = "local";

  private static final String LOCAL_DEP_VERSION = "1.0.0-LOCAL";

  // Use instance of Dependency Factory and clean it up b/w runs
  private static HashMap<ExternalDependency, Set<VersionlessDependency>> unresolvedToVersionless =
      new HashMap<>();

  private static HashMap<OResolvedDependency, OExternalDependency> externalDependencyCache =
      new HashMap<>();

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
  public static synchronized OExternalDependency from(
      String group,
      String name,
      String version,
      File dependencyFile,
      @Nullable File dependencySourceFile,
      ExternalDependenciesExtension externalDependenciesExtension,
      JetifierExtension jetifierExtension) {
    String classifier = DependencyUtils.getModuleClassifier(dependencyFile.getName(), version);

    VersionlessDependency versionlessDependency =
        VersionlessDependency.builder()
            .setGroup(group)
            .setName(name)
            .setClassifier(Optional.ofNullable(Strings.emptyToNull(classifier)))
            .build();

    OResolvedDependency resolvedDependency =
        OResolvedDependency.builder()
            .setVersionless(versionlessDependency)
            .setVersion(version)
            .setIsVersioned(externalDependenciesExtension.isVersioned(versionlessDependency))
            .setRealDependencyFile(dependencyFile)
            .setRealDependencySourceFile(Optional.ofNullable(dependencySourceFile))
            .build();

    if (externalDependencyCache.containsKey(resolvedDependency)) {
      return externalDependencyCache.get(resolvedDependency);
    }

    OExternalDependency externalDependency;

    if (group.equals(LOCAL_GROUP) || isLocalDependency(dependencyFile.getAbsolutePath())) {
      externalDependency =
          new LocalOExternalDependency(
              resolvedDependency, externalDependenciesExtension, jetifierExtension);
    } else {
      externalDependency =
          new OExternalDependency(
              resolvedDependency, externalDependenciesExtension, jetifierExtension);
    }

    externalDependencyCache.put(resolvedDependency, externalDependency);
    return externalDependency;
  }

  /**
   * Create an External Dependency from a local dependency
   *
   * @param localDependency local dependency file
   * @param externalDependenciesExtension External Dependency Extension
   * @param jetifierExtension Jetifier Extension
   * @return External Dependency
   */
  public static LocalOExternalDependency fromLocal(
      File localDependency,
      @Nullable File localSourceDependency,
      ExternalDependenciesExtension externalDependenciesExtension,
      JetifierExtension jetifierExtension) {

    String name = FilenameUtils.getBaseName(localDependency.getName());
    return (LocalOExternalDependency)
        from(
            LOCAL_GROUP,
            name,
            LOCAL_DEP_VERSION,
            localDependency,
            localSourceDependency,
            externalDependenciesExtension,
            jetifierExtension);
  }

  public static synchronized Set<VersionlessDependency> fromDependency(
      ExternalDependency dependency) {
    if (unresolvedToVersionless.containsKey(dependency)) {
      return unresolvedToVersionless.get(dependency);
    } else {
      VersionlessDependency.Builder vDependencyBuilder =
          VersionlessDependency.builder().setName(dependency.getName());
      String group = dependency.getGroup();

      if (group == null) {
        vDependencyBuilder.setGroup(LOCAL_GROUP);
      } else {
        vDependencyBuilder.setGroup(group);
      }

      Set<VersionlessDependency> vDeps = new HashSet<>();

      if (dependency.getArtifacts().size() > 0) {
        vDeps.addAll(
            dependency
                .getArtifacts()
                .stream()
                .map(
                    dependencyArtifact ->
                        vDependencyBuilder
                            .setClassifier(Optional.ofNullable(dependencyArtifact.getClassifier()))
                            .build())
                .collect(Collectors.toSet()));
      } else {
        vDeps.add(vDependencyBuilder.build());
      }

      unresolvedToVersionless.put(dependency, vDeps);
      return vDeps;
    }
  }

  /**
   * Returns a set of versionless dependency from the given gradle resolved dependency.
   *
   * @param dependency gradle dependency
   * @return VersionlessDependency object
   */
  public static Set<VersionlessDependency> fromDependency(ResolvedDependency dependency) {
    return dependency
        .getModuleArtifacts()
        .stream()
        .map(
            resolvedArtifact -> {
              if (resolvedArtifact.getId().getComponentIdentifier()
                  instanceof ProjectComponentIdentifier) {
                return null;
              } else {
                return VersionlessDependency.builder()
                    .setName(dependency.getModuleName())
                    .setGroup(dependency.getModuleGroup())
                    .setClassifier(Optional.ofNullable(resolvedArtifact.getClassifier()))
                    .build();
              }
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
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

  public static void cleanup() {
    unresolvedToVersionless = new HashMap<>();
    externalDependencyCache = new HashMap<>();
  }
}
