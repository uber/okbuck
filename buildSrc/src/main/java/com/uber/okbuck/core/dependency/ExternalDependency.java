package com.uber.okbuck.core.dependency;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.uber.okbuck.extension.ExternalDependenciesExtension;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.gradle.api.Project;

/**
 * Represents a pre packaged dependency from an external source like gradle/maven cache or the
 * filesystem
 */
public final class ExternalDependency {

  public static final String AAR = "aar";
  public static final String JAR = "jar";

  private static final String SOURCE_FILE = "-sources.jar";
  private static final String LINT_FILE = "-lint.jar";
  private static final String LOCAL_DEP_VERSION = "1.0.0-LOCAL";
  private static final String LOCAL_GROUP = "local";

  public final BaseExternalDependency base;

  @Nullable private Path realSourceFilePath;
  private boolean sourceFileInitialized;

  @Nullable private Path realLintFilePath;
  private boolean lintFileInitialized;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExternalDependency that = (ExternalDependency) o;
    return Objects.equals(base, that.base);
  }

  @Override
  public int hashCode() {
    return this.base.hashCode();
  }

  @Override
  public String toString() {
    return this.base.toString();
  }

  /** Returns the group of the dependency. */
  public String getGroup() {
    return this.base.versionless().group();
  }

  /** Returns the name of the dependency. */
  public String getName() {
    return this.base.versionless().name();
  }

  /** Returns the VersionlessDependency of the dependency. */
  public VersionlessDependency getVersionless() {
    return this.base.versionless();
  }

  /** Returns the version of the dependency. */
  public String getVersion() {
    return this.base.version();
  }

  /** Returns the real artifact file of the dependency. */
  public File getRealDependencyFile() {
    return this.base.realDependencyFile();
  }

  /** Returns the packaging of the the dependency: jar, aar, pex */
  public String getPackaging() {
    return this.base.packaging();
  }

  /** Returns the maven coordinates of the the dependency. */
  public String getMavenCoords() {
    return this.base.getMavenCoords();
  }

  /** Returns the cached base path of the dependency. */
  public Path getBasePath() {
    return this.base.basePath();
  }

  /** Returns the rule name the cached dependency file. */
  public String getCacheName() {
    return this.base.cacheName();
  }

  /** Returns the rule name the cached lint jar file. */
  public String getLintCacheName() {
    return getCacheName() + "-lint";
  }

  /** Returns the cached file name of the artifact of the dependency. */
  public String getDependencyFileName() {
    return getCacheName() + "." + getPackaging();
  }

  /** Returns the cached file path of the artifact of the dependency. */
  public Path getDependencyFilePath() {
    return getBasePath().resolve(this.getCacheName());
  }

  /** Returns the cached file name of the sources jar file. */
  public String getSourceFileName() {
    return getSourceFileNameFrom(getDependencyFileName());
  }

  /** Returns the cached file path of the sources jar file. */
  public Path getSourceFilePath() {
    return getBasePath().resolve(getSourceFileName());
  }

  /** Returns the cached lint file name. */
  public String getLintFileName() {
    return getDependencyFileName().replaceFirst("\\.aar$", LINT_FILE);
  }

  /** Returns the cached file path of the lint jar file. */
  public Path getLintFilePath() {
    return this.base.basePath().resolve(getLintCacheName());
  }

  /**
   * Gets the real sources jar path for a dependency if it exists.
   *
   * @param project The Project
   */
  @Nullable
  public Path getRealSourceFilePath(Project project) {
    if (!sourceFileInitialized) {
      realSourceFilePath = computeSourceFile(project);
      sourceFileInitialized = true;
    }
    return realSourceFilePath;
  }

  /** Check whether the dependency has a sources jar file. */
  public boolean hasSourceFile() {
    return realSourceFilePath != null;
  }

  /** Returns the real path of the lint jar file if present, null otherwise. */
  @Nullable
  public Path getRealLintFilePath() {
    if (!lintFileInitialized) {
      if (getPackaging().equals(AAR)) {
        realLintFilePath =
            DependencyUtils.getContentPath(getRealDependencyFile().toPath(), "lint.jar");
      } else {
        realLintFilePath = null;
      }
      lintFileInitialized = true;
    }
    return realLintFilePath;
  }

  /** Whether the external dependency has a lint jar file */
  public boolean hasLintFile() {
    return realLintFilePath != null;
  }

  @Nullable
  private Path computeSourceFile(Project project) {
    if (!DependencyUtils.isWhiteListed(getRealDependencyFile())
        && ImmutableList.of(JAR, AAR).contains(getPackaging())) {

      String sourceFileName = getSourceFileNameFrom(getRealDependencyFile().getName());

      Path sourcesJar = getRealDependencyFile().getParentFile().toPath().resolve(sourceFileName);

      if (Files.exists(sourcesJar)) {
        return sourcesJar;
      } else if (!base.isLocal()) {
        // Most likely jar is in Gradle/Maven cache directory,
        // try to find sources jar in "jar/../..".
        return DependencyUtils.getSingleZipFilePath(
            project, getRealDependencyFile().getParentFile().getParentFile(), sourceFileName);
      }
    }
    return null;
  }

  private String getSourceFileNameFrom(String prebuiltName) {
    if (ImmutableList.of(JAR, AAR).contains(getPackaging())) {
      return prebuiltName.replaceFirst("\\.(jar|aar)$", SOURCE_FILE);
    }
    throw new RuntimeException("Couldn't get sources file name for " + prebuiltName);
  }

  private ExternalDependency(
      String group,
      String name,
      @Nullable String version,
      @Nullable String classifier,
      File depFile,
      boolean isLocal,
      ExternalDependenciesExtension extension) {
    if (Strings.isNullOrEmpty(version)) {
      version = LOCAL_DEP_VERSION;
    }

    VersionlessDependency versionlessDependency =
        VersionlessDependency.builder()
            .setGroup(group)
            .setName(name)
            .setClassifier(Optional.ofNullable(Strings.emptyToNull(classifier)))
            .build();

    this.base =
        BaseExternalDependency.builder()
            .setVersionless(versionlessDependency)
            .setVersion(version)
            .setIsLocal(isLocal)
            .setIsVersioned(extension.isVersioned(versionlessDependency))
            .setRealDependencyFile(depFile)
            .build();
  }

  /**
   * Create an External Dependency
   *
   * @param group group of the dependency
   * @param name name of the dependency
   * @param version version of the dependency
   * @param dependencyFile file of the dependency
   * @param extension External Dependency Extension
   * @return External Dependency
   */
  public static ExternalDependency from(
      String group,
      String name,
      @Nullable String version,
      File dependencyFile,
      ExternalDependenciesExtension extension) {
    String classifier = DependencyUtils.getModuleClassifier(dependencyFile.getName(), version);
    return new ExternalDependency(
        group, name, version, classifier, dependencyFile, false, extension);
  }

  /**
   * Create an External Dependency from a local dependency
   *
   * @param localDep local dependency file
   * @param extension ExternalDependenciesExtension
   * @return External Dependency
   */
  public static ExternalDependency fromLocal(
      File localDep, ExternalDependenciesExtension extension) {
    return new ExternalDependency(
        LOCAL_GROUP,
        FilenameUtils.getBaseName(localDep.getName()),
        LOCAL_DEP_VERSION,
        null,
        localDep,
        true,
        extension);
  }
}
