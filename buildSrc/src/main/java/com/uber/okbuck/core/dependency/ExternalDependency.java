package com.uber.okbuck.core.dependency;

import static com.uber.okbuck.core.dependency.BaseExternalDependency.AAR;
import static com.uber.okbuck.core.dependency.BaseExternalDependency.JAR;

import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.uber.okbuck.extension.ExternalDependenciesExtension;
import com.uber.okbuck.extension.JetifierExtension;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.gradle.api.artifacts.Dependency;

/**
 * Represents a pre packaged dependency from an external source like gradle/maven cache or the
 * filesystem
 */
public class ExternalDependency {
  private static final String SOURCE_FILE = "-sources.jar";

  private final BaseExternalDependency base;
  private final Path cachePath;

  private boolean enableJetifier;
  private Set<ExternalDependency> dependencies = new HashSet<>();

  public static Comparator<ExternalDependency> compareByName =
      (o1, o2) ->
          ComparisonChain.start()
              .compare(o1.getPackaging(), o2.getPackaging())
              .compare(o1.getTargetName(), o2.getTargetName())
              .result();

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

  /**
   * Return the maven coordinates used for version validation. This excludes the classifier and
   * packaging attributes
   */
  public String getMavenCoordsForValidation() {
    return base.getMavenCoordsForValidation();
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
    return getVersionless().group();
  }

  /** Returns the name of the dependency. */
  public String getName() {
    return getVersionless().name();
  }

  /** Returns the VersionlessDependency of the dependency. */
  public VersionlessDependency getVersionless() {
    return this.base.versionless();
  }

  /** Returns the dependency as a Gradle Dependency. */
  public Dependency getAsGradleDependency() {
    return this.base.asGradleDependency();
  }

  /** Returns the version of the dependency. */
  public String getVersion() {
    return this.base.version();
  }

  /** Returns the real artifact file of the dependency. */
  public File getRealDependencyFile() {
    return this.base.realDependencyFile();
  }

  /** Gets the real sources jar file for the dependency. */
  public Optional<File> getRealSourceFile() {
    // Doesn't support sources for artifacts with classifier
    if (this.base.versionless().classifier().isPresent()) {
      return Optional.empty();
    } else {
      return this.base.realDependencySourceFile();
    }
  }

  public static String getGradleSha(File file) {
    return file.getParentFile().getName();
  }

  /** Returns the packaging of the the dependency: jar, aar, pex */
  public String getPackaging() {
    return this.base.packaging();
  }

  /** Returns the maven coordinates of the the dependency. */
  public String getMavenCoords() {
    return this.base.getMavenCoords();
  }

  /** Returns the target name of the dependency. */
  public String getTargetName() {
    return this.base.targetName() + "." + getPackaging();
  }

  /** Returns the target name of the dependency without packaging. */
  public String getBaseTargetName() {
    return this.base.targetName();
  }

  /** Returns the versionless target name of the dependency. */
  public String getVersionlessTargetName() {
    return this.base.versionlessTargetName() + "." + getPackaging();
  }

  /** Returns the target path of the dependency. */
  public String getTargetPath() {
    return cachePath.resolve(this.base.basePath()).toString();
  }

  /** Returns the cached file name of the sources jar file. */
  public String getSourceFileName() {
    return getSourceFileNameFrom(getDependencyFileName());
  }

  /** Returns the cached file name of the artifact of the dependency. */
  public String getDependencyFileName() {
    return getTargetName();
  }

  /** Returns true if this dependency needs to be jetified. */
  public boolean enableJetifier() {
    return enableJetifier;
  }

  public void setDeps(Set<ExternalDependency> dependencies) {
    this.dependencies = dependencies;
  }

  public Set<ExternalDependency> getDeps() {
    return dependencies;
  }

  String getSourceFileNameFrom(String prebuiltName) {
    if (ImmutableList.of(JAR, AAR).contains(getPackaging())) {
      return prebuiltName.replaceFirst("\\.(jar|aar)$", SOURCE_FILE);
    }
    throw new RuntimeException("Couldn't get sources file name for " + prebuiltName);
  }

  ExternalDependency(
      String group,
      String name,
      String version,
      @Nullable String classifier,
      File dependencyFile,
      @Nullable File dependencySourceFile,
      ExternalDependenciesExtension externalDependenciesExtension,
      JetifierExtension jetifierExtension) {
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
            .setIsVersioned(externalDependenciesExtension.isVersioned(versionlessDependency))
            .setRealDependencyFile(dependencyFile)
            .setRealDependencySourceFile(Optional.ofNullable(dependencySourceFile))
            .build();

    this.enableJetifier = jetifierExtension.shouldJetify(group, name, getPackaging());
    this.cachePath = Paths.get(externalDependenciesExtension.getCache());
  }

  public static Set<ExternalDependency> filterAar(Set<ExternalDependency> dependencies) {
    return dependencies
        .stream()
        .filter(dependency -> dependency.getPackaging().equals(AAR))
        .collect(Collectors.toSet());
  }

  public static Set<ExternalDependency> filterJar(Set<ExternalDependency> dependencies) {
    return dependencies
        .stream()
        .filter(dependency -> dependency.getPackaging().equals(JAR))
        .collect(Collectors.toSet());
  }
}
