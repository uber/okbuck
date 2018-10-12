package com.uber.okbuck.core.dependency;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.apache.commons.io.FilenameUtils;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.internal.artifacts.dependencies.DefaultDependencyArtifact;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;

@AutoValue
public abstract class BaseExternalDependency {

  private static final String CACHE_DELIMITER = "-";

  public abstract VersionlessDependency versionless();

  public abstract String version();

  public abstract boolean isLocal();

  public abstract File realDependencyFile();

  abstract boolean isVersioned();

  public static Builder builder() {
    return new AutoValue_BaseExternalDependency.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setVersionless(VersionlessDependency value);

    public abstract Builder setVersion(String value);

    public abstract Builder setIsLocal(boolean value);

    public abstract Builder setIsVersioned(boolean value);

    public abstract Builder setRealDependencyFile(File value);

    public abstract BaseExternalDependency build();
  }

  @Override
  public String toString() {
    return this.getMavenCoords() + " -> " + realDependencyFile().toString();
  }

  @Memoized
  public String getMavenCoords() {
    return versionless().group()
        + VersionlessDependency.COORD_DELIMITER
        + versionless().name()
        + VersionlessDependency.COORD_DELIMITER
        + packaging()
        + VersionlessDependency.COORD_DELIMITER
        + version()
        + versionless().classifier().map(c -> VersionlessDependency.COORD_DELIMITER + c).orElse("");
  }

  @Memoized
  public String packaging() {
    return FilenameUtils.getExtension(realDependencyFile().getName());
  }

  @Memoized
  public String cacheName() {
    StringBuilder cacheName = new StringBuilder(versionless().name());
    if (isVersioned()) {
      cacheName.append(CACHE_DELIMITER).append(version());
    }
    cacheName.append(versionless().classifier().map(c -> CACHE_DELIMITER + c).orElse(""));

    return cacheName.toString();
  }

  @Memoized
  Path basePath() {
    return Paths.get(versionless().group().replace('.', File.separatorChar));
  }

  @Memoized
  Dependency asGradleDependency() {
    // Internal class
    DefaultExternalModuleDependency externalDependency =
        new DefaultExternalModuleDependency(versionless().group(), versionless().name(), version());

    // Set transitive to false since this should just represent itself.
    externalDependency.setTransitive(false);

    // Internal class - Taken from ModuleFactoryHelper.java in gradle.
    Optional<String> classifier = versionless().classifier();
    if (classifier.isPresent()) {
      DependencyArtifact artifact =
          new DefaultDependencyArtifact(
              externalDependency.getName(), packaging(), packaging(), classifier.get(), null);
      externalDependency.addArtifact(artifact);
    }

    return externalDependency;
  }
}
