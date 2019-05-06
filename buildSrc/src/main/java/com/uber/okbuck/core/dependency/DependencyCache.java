package com.uber.okbuck.core.dependency;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.manager.DependencyManager;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.ExternalDependenciesExtension;
import com.uber.okbuck.extension.JetifierExtension;
import com.uber.okbuck.extension.OkBuckExtension;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyCache {

  private static final Logger LOG = LoggerFactory.getLogger(DependencyCache.class);
  private final Project rootProject;
  private final DependencyManager dependencyManager;
  private final boolean skipPrebuilt;
  private final Map<VersionlessDependency, ExternalDependency> forcedDeps = new HashMap<>();

  public DependencyCache(
      Project project,
      DependencyManager dependencyManager,
      boolean skipPrebuilt,
      @Nullable String forcedConfiguration) {
    this.rootProject = project.getRootProject();
    this.dependencyManager = dependencyManager;
    this.skipPrebuilt = skipPrebuilt;

    if (forcedConfiguration != null) {
      build(forcedConfiguration)
          .forEach(
              dependency -> {
                forcedDeps.put(dependency.getVersionless(), dependency);
              });
    }
  }

  public DependencyCache(
      Project project, DependencyManager dependencyManager, @Nullable String forcedConfiguration) {
    this(project, dependencyManager, false, forcedConfiguration);
  }

  public DependencyCache(Project project, DependencyManager dependencyManager) {
    this(project, dependencyManager, false);
  }

  public DependencyCache(
      Project project, DependencyManager dependencyManager, boolean skipPrebuilt) {
    this(project, dependencyManager, skipPrebuilt, null);
  }

  public final ExternalDependency get(ExternalDependency externalDependency) {
    LOG.info("Requested dependency {}", externalDependency);
    ExternalDependency dependency =
        forcedDeps.getOrDefault(externalDependency.getVersionless(), externalDependency);
    LOG.info("Picked dependency {}", dependency);

    dependencyManager.addDependency(dependency, skipPrebuilt);

    return dependency;
  }

  public final void addDependencies(DependencySet dependencySet) {
    this.dependencyManager.addDependencies(
        dependencySet
            .stream()
            .filter(dependency -> dependency instanceof org.gradle.api.artifacts.ExternalDependency)
            .filter(dependency -> dependency.getGroup() != null && dependency.getVersion() != null)
            .map(dependency -> (org.gradle.api.artifacts.ExternalDependency) dependency)
            .collect(Collectors.toSet()));
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
  public boolean hasAutoValueExtension(ExternalDependency externalDependency) {
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
   * Use this method to populate dependency caches of tools/languages etc. This is not meant to be
   * used across multiple threads/gradle task executions which can run in parallel. This method is
   * fully synchronous.
   *
   * @param configuration The configuration to materialize into the dependency cache
   */
  public Set<ExternalDependency> build(Configuration configuration) {
    OkBuckExtension okBuckExtension = ProjectUtil.getOkBuckExtension(rootProject);

    ExternalDependenciesExtension externalDependenciesExtension =
        okBuckExtension.getExternalDependenciesExtension();
    JetifierExtension jetifierExtension = okBuckExtension.getJetifierExtension();

    addDependencies(configuration.getAllDependencies());

    return DependencyUtils.resolveExternal(
            rootProject, configuration, externalDependenciesExtension, jetifierExtension)
        .stream()
        .map(this::get)
        .collect(Collectors.toSet());
  }

  private Set<ExternalDependency> build(String configuration) {
    Configuration useful = DependencyUtils.useful(configuration, rootProject);

    Preconditions.checkNotNull(useful);

    return build(useful);
  }
}
