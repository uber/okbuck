package com.uber.okbuck.core.dependency;

import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.manager.DependencyManager;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.base.Store;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.ExternalExtension;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
  private final File cacheDir;
  private final Project rootProject;
  private final com.uber.okbuck.core.manager.DependencyManager dependencyManager;
  private final boolean fetchSources;
  private final Store processors;
  private final Store processorExtensions;
  private final Set<ExternalDependency> requested = ConcurrentHashMap.newKeySet();
  private final Map<VersionlessDependency, ExternalDependency> forcedDeps = new HashMap<>();

  public DependencyCache(
      Project project,
      File cacheDir,
      com.uber.okbuck.core.manager.DependencyManager dependencyManager,
      @Nullable String forcedConfiguration) {
    this.rootProject = project.getRootProject();
    this.cacheDir = cacheDir;
    this.dependencyManager = dependencyManager;
    this.fetchSources = ProjectUtil.getOkBuckExtension(project).getIntellijExtension().sources;

    processors = new Store(rootProject.file(OkBuckGradlePlugin.OKBUCK_STATE_DIR + "/PROCESSORS"));
    processorExtensions =
        new Store(rootProject.file(OkBuckGradlePlugin.OKBUCK_STATE_DIR + "/PROCESSORS_EXTENSIONS"));

    if (forcedConfiguration != null) {
      Scope.builder(project)
          .configuration(forcedConfiguration)
          .build()
          .getExternal()
          .forEach(
              dependency -> {
                get(dependency);
                forcedDeps.put(dependency.versionless, dependency);
              });
    }
  }

  public DependencyCache(Project project, File cacheDir, DependencyManager dependencyManager) {
    this(project, cacheDir, dependencyManager, null);
  }

  public void finalizeDeps() {
    LOG.info("Finalizing Dependency Cache");
    processors.persist();
    processorExtensions.persist();
    cleanup();
  }

  public void cleanup() {
    String changingDeps =
        requested
            .stream()
            .filter(
                dependency -> {
                  String version = dependency.version;
                  return version.endsWith("+") || version.endsWith("-SNAPSHOT");
                })
            .map(ExternalDependency::getCacheName)
            .collect(Collectors.joining("\n"));

    if (!changingDeps.isEmpty()) {
      String message =
          "Please do not use changing dependencies. They can cause hard to reproduce builds.\n"
              + changingDeps;
      if (ProjectUtil.getOkBuckExtension(rootProject).failOnChangingDependencies) {
        throw new IllegalStateException(message);
      } else {
        LOG.warn(message);
      }
    }
  }

  public ExternalDependency get(ExternalDependency externalDependency, boolean resolveOnly) {
    LOG.info("Requested dependency {}", externalDependency);
    ExternalDependency dependency =
        forcedDeps.getOrDefault(externalDependency.versionless, externalDependency);
    LOG.info("Picked dependency {}", dependency);

    dependencyManager.addDependency(dependency);

    if (!resolveOnly && fetchSources) {
      LOG.info("Fetching sources for {}", dependency);
      dependency.getSourceJar(rootProject);
    }
    requested.add(dependency);

    return dependency;
  }

  public ExternalDependency get(ExternalDependency externalDependency) {
    return get(externalDependency, false);
  }

  public String getPath(ExternalDependency dependency) {
    File cachedCopy = cacheDir.toPath().resolve(dependency.getDepFilePath()).toFile();

    return FileUtil.getRelativePath(rootProject.getProjectDir(), cachedCopy);
  }

  /**
   * Get the list of annotation processor classes provided by a dependency.
   *
   * @param externalDependency The dependency
   * @return The list of annotation processor classes available in the manifest
   */
  public Set<String> getAnnotationProcessors(ExternalDependency externalDependency) {
    ExternalDependency dependency =
        forcedDeps.getOrDefault(externalDependency.versionless, externalDependency);
    String key = dependency.getCacheName();
    String processorsList = processors.get(key);

    if (processorsList == null) {
      try {
        processorsList =
            getJarFileContent(
                dependency.depFile, "META-INF/services/javax.annotation.processing.Processor");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      processors.set(key, processorsList);
    }

    if (processorsList.isEmpty()) {
      return ImmutableSet.of();
    } else {
      return ImmutableSet.copyOf(processorsList.split(","));
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
        forcedDeps.getOrDefault(externalDependency.versionless, externalDependency);
    String key = dependency.getCacheName();
    String extensions = processorExtensions.get(key);

    if (extensions == null) {
      try {
        extensions =
            getJarFileContent(
                dependency.depFile,
                "META-INF/services/com.google.auto.value.extension.AutoValueExtension");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      processorExtensions.set(key, extensions);
    }

    return !extensions.isEmpty();
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
        forcedDeps.getOrDefault(externalDependency.versionless, externalDependency);
    if (dependency.getLintJar() != null) {
      File cachedCopy =
          cacheDir
              .toPath()
              .resolve(dependency.getGroup().replace('.', File.separatorChar))
              .resolve(dependency.getLintCacheName())
              .toFile();

      return FileUtil.getRelativePath(rootProject.getProjectDir(), cachedCopy);
    }
    return null;
  }

  public Set<String> build(Configuration configuration, boolean cleanupDeps) {
    return build(Collections.singleton(configuration), cleanupDeps);
  }

  public Set<String> build(Configuration configuration) {
    return build(configuration, true);
  }

  /**
   * Use this method to populate dependency caches of tools/languages etc. This is not meant to be
   * used across multiple threads/gradle task executions which can run in parallel. This method is
   * fully synchronous.
   *
   * @param configurations The set of configurations to materialize into the dependency cache
   */
  Set<String> build(Set<Configuration> configurations, boolean cleanupDeps) {
    ExternalExtension externalExtension =
        ProjectUtil.getOkBuckExtension(rootProject).getExternalExtension();
    Set<String> dependencies =
        configurations
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
                                    new ExternalDependency(
                                        moduleIdentifier.getGroup(),
                                        moduleIdentifier.getModule(),
                                        moduleIdentifier.getVersion(),
                                        artifact.getFile(),
                                        externalExtension);
                              } else {
                                dependency =
                                    ExternalDependency.fromLocal(
                                        artifact.getFile(), externalExtension);
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

    if (cleanupDeps) {
      cleanup();
    }

    return dependencies;
  }

  private IllegalStateException artifactResolveException(Exception e) {
    return new IllegalStateException(
        "Failed to resolve an artifact. Make sure you have a repositories block defined. "
            + "See https://github.com/uber/okbuck/wiki/Known-caveats#could-not-resolve-all-"
            + "dependencies-for-configuration for more information.",
        e);
  }

  /**
   * Use this method to populate dependency caches of tools/languages etc. This is not meant to be
   * used across multiple threads/gradle task executions which can run in parallel. This method is
   * fully synchronous.
   *
   * @param configurations The set of configurations to materialize into the dependency cache
   */
  public Set<String> build(Set<Configuration> configurations) {
    return build(configurations, true);
  }
}
