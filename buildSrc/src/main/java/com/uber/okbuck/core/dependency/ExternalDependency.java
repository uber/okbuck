package com.uber.okbuck.core.dependency;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.uber.okbuck.extension.ExternalDependencyExtension;
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

  public String getGroup() {
    return this.base.versionless().group();
  }

  public String getName() {
    return this.base.versionless().name();
  }

  public VersionlessDependency getVersionless() {
    return this.base.versionless();
  }

  public String getVersion() {
    return this.base.version();
  }

  public File getRealDependencyFile() {
    return this.base.realDependencyFile();
  }

  public String getPackaging() {
    return this.base.packaging();
  }

  public String getMavenCoords() {
    return this.base.getMavenCoords();
  }

  public Path getBasePath() {
    return this.base.basePath();
  }

  public String getCacheName() {
    return this.base.cacheName();
  }

  public String getLintCacheName() {
    return getCacheName() + "-lint";
  }

  public String getDependencyFileName() {
    return getCacheName() + "." + getPackaging();
  }

  public Path getDependencyFilePath() {
    return getBasePath().resolve(this.getCacheName());
  }

  public String getSourceFileName() {
    return getSourceFileNameFrom(getDependencyFileName());
  }

  public Path getSourceFilePath() {
    return getBasePath().resolve(getSourceFileName());
  }

  public String getLintFileName() {
    return getDependencyFileName().replaceFirst("\\.aar$", LINT_FILE);
  }

  public Path getLintFilePath() {
    return this.base.basePath().resolve(getLintCacheName());
  }

  /**
   * Gets the sources jar path for a dependency if it exists.
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

  public boolean hasSourceFile() {
    return realSourceFilePath != null;
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
        return DependencyUtils.getFilePath(
            project, getRealDependencyFile().getParentFile().getParentFile(), sourceFileName);
      }
    }
    return null;
  }

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

  public boolean hasLintFile() {
    return realLintFilePath != null;
  }

  private String getSourceFileNameFrom(String prebuiltName) {
    if (ImmutableList.of(JAR, AAR).contains(getPackaging())) {
      return prebuiltName.replaceFirst("\\.(jar|aar)$", SOURCE_FILE);
    }
    throw new RuntimeException("Couldn't get source file name for " + prebuiltName);
  }

  private ExternalDependency(
      String group,
      String name,
      @Nullable String version,
      @Nullable String classifier,
      File depFile,
      boolean isLocal,
      ExternalDependencyExtension extension) {

    // TODO find a better solution when group ends with 'buck'
    // which can cause it to clash with BUCK files generated at that path
    if (group.contains(".buck")) {
      group = group.replace(".buck", ".buckm");
    }

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

  public static ExternalDependency from(
      String group,
      String name,
      @Nullable String version,
      File dependencyFile,
      ExternalDependencyExtension extension) {
    String classifier = DependencyUtils.getModuleClassifier(dependencyFile.getName(), version);
    return new ExternalDependency(
        group, name, version, classifier, dependencyFile, false, extension);
  }

  public static ExternalDependency fromLocal(File localDep, ExternalDependencyExtension extension) {
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
