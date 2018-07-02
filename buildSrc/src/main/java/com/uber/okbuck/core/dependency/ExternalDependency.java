package com.uber.okbuck.core.dependency;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.uber.okbuck.extension.ExternalExtension;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.gradle.api.Project;
import org.gradle.api.file.FileTree;

/**
 * Represents a pre packaged dependency from an external source like gradle/maven cache or the
 * filesystem
 */
public final class ExternalDependency {

  private static final String LOCAL_DEP_VERSION = "1.0.0-LOCAL";
  private static final String LOCAL_GROUP = "local";
  private static final String SOURCE_FILE = "-sources.jar";
  private static final String LINT_FILE = "-lint.jar";

  /**
   * this is used to output artifacts as %GROUP%--%NAME%--%VERSION%--%CLASSIFIER% Thus making it
   * possible to re-construct maven coordinates.
   */
  private static final String CACHE_DELIMITER = "--";

  public final String version;
  public final File depFile;
  private final String group;
  public final String name;
  public final VersionlessDependency versionless;
  public final String packaging;

  public final Optional<String> classifier;

  public final ExternalExtension externalExtension;

  private Path sourceJar;
  private boolean sourceJarInitialized;

  private Path lintJar;
  private boolean lintJarInitialized;

  final boolean isLocal;

  public ExternalDependency(
      String group,
      String name,
      @Nullable String version,
      File depFile,
      ExternalExtension externalExtension) {
    this(group, name, version, null, depFile, false, externalExtension);
  }

  public ExternalDependency(
      String group,
      String name,
      @Nullable String version,
      String classifier,
      File depFile,
      ExternalExtension externalExtension) {
    this(group, name, version, classifier, depFile, false, externalExtension);
  }

  public String getGroup() {
    return group;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExternalDependency that = (ExternalDependency) o;
    return Objects.equals(classifier, that.classifier)
        && Objects.equals(version, that.version)
        && Objects.equals(group, that.group)
        && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, group, name);
  }

  @Override
  public String toString() {
    return this.getMavenCoords() + " -> " + this.depFile.toString();
  }

  public String getCacheName() {
    StringBuilder cacheName = new StringBuilder(name);
    if (externalExtension.isAllowed(this.versionless)) {
      cacheName.append(CACHE_DELIMITER).append(version);
    }
    cacheName.append(classifier.map(c -> CACHE_DELIMITER + c).orElse(""));

    return cacheName.toString();
  }

  public String getVersion() {
    return version;
  }

  public String getVersionless() {
    return versionless.toString();
  }

  public String getLintCacheName() {
    return getCacheName() + "-lint";
  }

  public String getMavenCoords() {
    return group
        + VersionlessDependency.COORD_DELIMITER
        + name
        + VersionlessDependency.COORD_DELIMITER
        + packaging
        + VersionlessDependency.COORD_DELIMITER
        + String.valueOf(version)
        + classifier.map(c -> VersionlessDependency.COORD_DELIMITER + c).orElse("");
  }

  public String getDepFileName() {
    return getCacheName() + "." + packaging;
  }

  public Path getDepFilePath() {
    return Paths.get(this.getGroup().replace('.', File.separatorChar)).resolve(this.getCacheName());
  }

  public String getSourceFileName() {
    return sourced(getDepFileName());
  }

  public String getLintFileName() {
    return getDepFileName().replaceFirst("\\.aar$", LINT_FILE);
  }

  private static String sourced(String name) {
    return name.replaceFirst("\\.(jar|aar)$", SOURCE_FILE);
  }

  /**
   * Gets the sources jar path for a dependency if it exists.
   *
   * @param project The Project
   */
  private Path computeSourceJar(Project project) {
    if (!DependencyUtils.isWhiteListed(depFile)
        && FilenameUtils.isExtension(depFile.getName(), ImmutableList.of("jar", "aar"))) {
      String sourcesJarName = sourced(depFile.getName());
      Path sourcesJar = depFile.getParentFile().toPath().resolve(sourcesJarName);

      if (Files.exists(sourcesJar)) {
        return sourcesJar;
      } else {
        if (!isLocal) {
          // Most likely jar is in Gradle/Maven cache directory,
          // try to find sources jar in "jar/../..".
          FileTree sourceJars =
              project.fileTree(
                  ImmutableMap.of(
                      "dir", depFile.getParentFile().getParentFile().getAbsolutePath(),
                      "includes", ImmutableList.of("**/" + sourcesJarName)));

          try {
            return sourceJars.getSingleFile().toPath();
          } catch (IllegalStateException ignored) {
            if (sourceJars.getFiles().size() > 1) {
              throw new IllegalStateException(
                  "Found multiple source jars: " + sourceJars + " for " + this);
            }
          }
        }
      }
    }
    return null;
  }

  public Path getSourceJar(Project project) {
    if (!sourceJarInitialized) {
      sourceJar = computeSourceJar(project);
      sourceJarInitialized = true;
    }
    return sourceJar;
  }

  public boolean hasSourceJar() {
    return sourceJar != null;
  }

  private Path computeLintJar() {
    if (packaging.equals("aar")) {
      try {
        FileSystem zipFile = FileSystems.newFileSystem(depFile.toPath(), null);
        Path packagedPath = zipFile.getPath("lint.jar");
        if (Files.exists(packagedPath)) {
          return packagedPath;
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  public Path getLintJar() {
    if (!lintJarInitialized) {
      lintJar = computeLintJar();
      lintJarInitialized = true;
    }
    return lintJar;
  }

  public boolean hasLintJar() {
    return lintJar != null;
  }

  private ExternalDependency(
      String group,
      String name,
      @Nullable String version,
      @Nullable String classifier,
      File depFile,
      boolean isLocal,
      ExternalExtension externalExtension) {

    // TODO find a better solution when group ends with 'buck'
    // which can cause it to clash with BUCK files generated at that path
    if (group.contains(".buck")) {
      this.group = group.replace(".buck", ".buckm");
    } else {
      this.group = group;
    }

    this.name = name;
    this.isLocal = isLocal;
    if (!Strings.isNullOrEmpty(version)) {
      this.version = version;
    } else {
      this.version = LOCAL_DEP_VERSION;
    }
    this.classifier = Optional.ofNullable(Strings.emptyToNull(classifier));

    this.depFile = depFile;
    this.versionless = new VersionlessDependency(group, name, this.classifier);
    this.packaging = FilenameUtils.getExtension(depFile.getName());

    this.externalExtension = externalExtension;
  }

  public static ExternalDependency fromLocal(File localDep, ExternalExtension externalExtension) {
    String baseName = FilenameUtils.getBaseName(localDep.getName());
    return new ExternalDependency(
        LOCAL_GROUP, baseName, LOCAL_DEP_VERSION, null, localDep, true, externalExtension);
  }
}
