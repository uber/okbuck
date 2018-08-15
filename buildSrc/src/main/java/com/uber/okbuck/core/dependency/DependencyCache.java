package com.uber.okbuck.core.dependency;

import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.manager.DependencyManager;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.ExternalDependenciesExtension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.DefaultLenientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyCache {

  private static final Logger LOG = LoggerFactory.getLogger(DependencyCache.class);
  private final Project rootProject;
  private final DependencyManager dependencyManager;
  private final boolean fetchSources;
  private final Map<VersionlessDependency, ExternalDependency> forcedDeps = new HashMap<>();

  public DependencyCache(
      Project project, DependencyManager dependencyManager, @Nullable String forcedConfiguration) {
    this.rootProject = project.getRootProject();
    this.dependencyManager = dependencyManager;
    this.fetchSources = ProjectUtil.getOkBuckExtension(project).getIntellijExtension().sources;

    if (forcedConfiguration != null) {
      Scope.builder(project)
          .configuration(forcedConfiguration)
          .build()
          .getExternal()
          .forEach(
              dependency -> {
                get(dependency);
                forcedDeps.put(dependency.getVersionless(), dependency);
              });
    }
  }

  public DependencyCache(Project project, DependencyManager dependencyManager) {
    this(project, dependencyManager, null);
  }

  public ExternalDependency get(ExternalDependency externalDependency, boolean resolveOnly) {
    LOG.info("Requested dependency {}", externalDependency);
    ExternalDependency dependency =
        forcedDeps.getOrDefault(externalDependency.getVersionless(), externalDependency);
    LOG.info("Picked dependency {}", dependency);

    dependencyManager.addDependency(dependency);

    if (!resolveOnly && fetchSources) {
      LOG.info("Fetching sources for {}", dependency);
      dependency.getRealSourceFilePath(rootProject);
    }

    return dependency;
  }

  public ExternalDependency get(ExternalDependency externalDependency) {
    return get(externalDependency, false);
  }

  public String getPath(ExternalDependency dependency) {
    return Paths.get(dependencyManager.getCacheDirName())
        .resolve(dependency.getDependencyFilePath())
        .toString();
  }

  /**
   * Get the list of annotation processor classes provided by a dependency.
   *
   * @param externalDependency The dependency
   * @return The list of annotation processor classes available in the manifest
   */
  public Set<String> getAnnotationProcessors(ExternalDependency externalDependency) {
    ExternalDependency dependency =
        forcedDeps.getOrDefault(externalDependency.getVersionless(), externalDependency);
    String key = dependency.getCacheName();

    try {
      String processors =
          getJarFileContent(
              dependency.getRealDependencyFile(),
              "META-INF/services/javax.annotation.processing.Processor");

      return ImmutableSet.copyOf(processors.split(","));

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Check if the dependency has an auto value extension.
   *
   * @param externalDependency The dependency
   * @return Whether the dependency has auto value extension.
   */
  public boolean hasAutoValueExtensions(ExternalDependency externalDependency) {
    ExternalDependency dependency =
        forcedDeps.getOrDefault(externalDependency.getVersionless(), externalDependency);
    try {
      String extensions =
          getJarFileContent(
              dependency.getRealDependencyFile(),
              "META-INF/services/com.google.auto.value.extension.AutoValueExtension");
      return !extensions.isEmpty();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String getJarFileContent(File dependencyFile, String filePathString)
      throws IOException {
    String content;

    JarFile jarFile = new JarFile(dependencyFile);
    JarEntry jarEntry = (JarEntry) jarFile.getEntry(filePathString);
    if (jarEntry != null) {
      List<String> entries =
          Arrays.stream(
                  IOUtils.toString(jarFile.getInputStream(jarEntry), "UTF-8").trim().split("\\n"))
              .filter(
                  entry -> {
                    return !entry.startsWith("#")
                        && !entry.trim().isEmpty(); // filter out comments and empty lines
                  })
              .collect(Collectors.toList());
      content = String.join(",", entries);
    } else {
      content = "";
    }
    return content;
  }

  /**
   * Get the packaged lint jar of an aar dependency if any.
   *
   * @param externalDependency The dependency
   * @return path to the lint jar in the cache.
   */
  @Nullable
  public String getLintJar(ExternalDependency externalDependency) {
    ExternalDependency dependency =
        forcedDeps.getOrDefault(externalDependency.getVersionless(), externalDependency);
    if (dependency.getRealLintFilePath() != null) {
      return Paths.get(dependencyManager.getCacheDirName())
          .resolve(dependency.getLintFilePath())
          .toString();
    }
    return null;
  }

  public Set<String> build(Configuration configuration) {
    return build(Collections.singleton(configuration));
  }

  /**
   * Use this method to populate dependency caches of tools/languages etc. This is not meant to be
   * used across multiple threads/gradle task executions which can run in parallel. This method is
   * fully synchronous.
   *
   * @param configurations The set of configurations to materialize into the dependency cache
   */
  private Set<String> build(Set<Configuration> configurations) {
    ExternalDependenciesExtension externalDependenciesExtension =
        ProjectUtil.getOkBuckExtension(rootProject).getExternalDependenciesExtension();

    return configurations
        .stream()
        .map(
            configuration -> {
              try {
                return configuration
                    .getIncoming()
                    .getArtifacts()
                    .getArtifacts()
                    .stream()
                    .map(
                        artifact -> {
                          ComponentIdentifier identifier =
                              artifact.getId().getComponentIdentifier();
                          if (identifier instanceof ProjectComponentIdentifier
                              || !DependencyUtils.isConsumable(artifact.getFile())) {
                            return null;
                          }
                          ExternalDependency dependency;
                          if (identifier instanceof ModuleComponentIdentifier
                              && ((ModuleComponentIdentifier) identifier).getVersion().length()
                                  > 0) {
                            ModuleComponentIdentifier moduleIdentifier =
                                (ModuleComponentIdentifier) identifier;
                            dependency =
                                ExternalDependency.from(
                                    moduleIdentifier.getGroup(),
                                    moduleIdentifier.getModule(),
                                    moduleIdentifier.getVersion(),
                                    artifact.getFile(),
                                    externalDependenciesExtension);
                          } else {
                            dependency =
                                ExternalDependency.fromLocal(
                                    artifact.getFile(), externalDependenciesExtension);
                          }
                          return get(dependency, true);
                        })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
              } catch (DefaultLenientConfiguration.ArtifactResolveException e) {
                throw artifactResolveException(e);
              }
            })
        .flatMap(Collection::stream)
        .map(this::getPath)
        .collect(Collectors.toSet());
  }

  private IllegalStateException artifactResolveException(Exception e) {
    return new IllegalStateException(
        "Failed to resolve an artifact. Make sure you have a repositories block defined. "
            + "See https://github.com/uber/okbuck/wiki/Known-caveats#could-not-resolve-all-"
            + "dependencies-for-configuration for more information.",
        e);
  }
}
