package com.uber.okbuck.core.manager;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.java.PrebuiltRuleComposer;
import com.uber.okbuck.core.dependency.DependencyUtils;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.dependency.VersionlessDependency;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.LoadStatementsUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.ExternalDependenciesExtension;
import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.extension.RuleOverridesExtension;
import com.uber.okbuck.template.core.Rule;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyManager {

  private static final Logger LOG = LoggerFactory.getLogger(DependencyManager.class);

  private final Project project;
  private final String cacheDirName;
  private final ExternalDependenciesExtension extension;
  private final RuleOverridesExtension overridesExtension;

  private final SetMultimap<VersionlessDependency, ExternalDependency> originalDependencyMap =
      LinkedHashMultimap.create();

  private final HashMap<VersionlessDependency, Boolean> skipPrebuiltDependencyMap = new HashMap<>();

  public DependencyManager(Project rootProject, OkBuckExtension okBuckExtension) {

    this.project = rootProject;
    this.cacheDirName = okBuckExtension.externalDependencyCache;
    this.extension = okBuckExtension.getExternalDependenciesExtension();
    this.overridesExtension = okBuckExtension.getRuleOverridesExtension();
  }

  public synchronized void addDependency(ExternalDependency dependency, boolean skipPrebuilt) {
    VersionlessDependency versionless = dependency.getVersionless();
    originalDependencyMap.put(versionless, dependency);

    if (skipPrebuiltDependencyMap.containsKey(versionless)) {
      skipPrebuiltDependencyMap.put(
          versionless, skipPrebuiltDependencyMap.get(versionless) && skipPrebuilt);
    } else {
      skipPrebuiltDependencyMap.put(versionless, skipPrebuilt);
    }
  }

  public String getCacheDirName() {
    return this.cacheDirName;
  }

  public void finalizeDependencies() {
    Map<VersionlessDependency, Collection<ExternalDependency>> filteredDependencyMap =
        filterDependencies();
    validateDependencies(filteredDependencyMap);
    processDependencies(filteredDependencyMap);
  }

  private File getCacheDir() {
    return project.getRootProject().file(cacheDirName);
  }

  private Map<VersionlessDependency, Collection<ExternalDependency>> filterDependencies() {
    if (!extension.allowLatestEnabled()) {
      return originalDependencyMap.asMap();
    }

    ImmutableMap.Builder<VersionlessDependency, Collection<ExternalDependency>>
        filteredDependencyMapBuilder = ImmutableMap.builder();

    ImmutableList.Builder<ExternalDependency> dependenciesToResolveBuilder =
        ImmutableList.builder();

    originalDependencyMap
        .asMap()
        .forEach(
            (key, value) -> {
              if (value.size() == 1) {
                // Already has one dependency, no need to resolve different versions.
                filteredDependencyMapBuilder.put(key, value);
              } else if (extension.isAllowLatestFor(key)) {
                dependenciesToResolveBuilder.addAll(value);
              } else {
                filteredDependencyMapBuilder.put(key, value);
              }
            });

    resolved(dependenciesToResolveBuilder.build())
        .forEach(
            externalDependency -> {
              filteredDependencyMapBuilder.put(
                  externalDependency.getVersionless(), ImmutableList.of(externalDependency));
            });

    return filteredDependencyMapBuilder.build();
  }

  private Set<ExternalDependency> resolved(Collection<ExternalDependency> externalDependencies) {
    Configuration detached =
        project
            .getConfigurations()
            .detachedConfiguration(
                externalDependencies
                    .stream()
                    .map(ExternalDependency::getAsGradleDependency)
                    .toArray(Dependency[]::new));
    return DependencyUtils.resolveExternal(detached, extension);
  }

  private void validateDependencies(
      Map<VersionlessDependency, Collection<ExternalDependency>> dependencyMap) {
    if (extension.versionlessEnabled()) {
      Joiner.MapJoiner mapJoiner = Joiner.on(",\n").withKeyValueSeparator("=");

      Map<String, Set<String>> extraDependencies =
          dependencyMap
              .entrySet()
              .stream()
              .filter(entry -> entry.getValue().size() > 1)
              .map(Map.Entry::getValue)
              .flatMap(Collection::stream)
              .filter(dependency -> !extension.isAllowedVersion(dependency))
              .collect(
                  Collectors.groupingBy(
                      dependency -> dependency.getVersionless().mavenCoords(),
                      Collectors.mapping(ExternalDependency::getVersion, Collectors.toSet())));

      if (extraDependencies.size() > 0) {
        throw new RuntimeException(
            "Multiple versions found for external dependencies: \n"
                + mapJoiner.join(extraDependencies));
      }

      Map<String, Set<String>> singleDependencies =
          dependencyMap
              .entrySet()
              .stream()
              .filter(entry -> entry.getValue().size() == 1)
              .map(Map.Entry::getValue)
              .flatMap(Collection::stream)
              .filter(dependency -> extension.isVersioned(dependency.getVersionless()))
              .collect(
                  Collectors.groupingBy(
                      dependency -> dependency.getVersionless().mavenCoords(),
                      Collectors.mapping(ExternalDependency::getVersion, Collectors.toSet())));

      if (singleDependencies.size() > 0) {
        throw new RuntimeException(
            "Single version found for external dependencies, please remove them from external dependency extension: \n"
                + mapJoiner.join(singleDependencies));
      }
    }

    String changingDeps =
        dependencyMap
            .values()
            .stream()
            .flatMap(Collection::stream)
            .filter(
                dependency -> {
                  String version = dependency.getVersion();
                  return version.endsWith("+") || version.endsWith("-SNAPSHOT");
                })
            .map(ExternalDependency::getCacheName)
            .collect(Collectors.joining("\n"));

    if (!changingDeps.isEmpty()) {
      String message =
          "Please do not use changing dependencies. They can cause hard to reproduce builds.\n"
              + changingDeps;
      if (ProjectUtil.getOkBuckExtension(project).failOnChangingDependencies) {
        throw new IllegalStateException(message);
      } else {
        LOG.warn(message);
      }
    }
  }

  private void processDependencies(
      Map<VersionlessDependency, Collection<ExternalDependency>> dependencyMap) {
    File cacheDir = getCacheDir();
    if (cacheDir.exists()) {
      try {
        FileUtils.deleteDirectory(cacheDir);
      } catch (IOException e) {
        throw new RuntimeException("Could not delete dependency directory: " + cacheDir);
      }
    }

    if (!cacheDir.mkdirs()) {
      throw new IllegalStateException("Couldn't create dependency directory: " + cacheDir);
    }

    Map<Path, List<ExternalDependency>> groupToDependencyMap =
        dependencyMap
            .values()
            .stream()
            .flatMap(Collection::stream)
            .collect(
                Collectors.groupingBy(
                    dependency -> cacheDir.toPath().resolve(dependency.getBasePath())));

    groupToDependencyMap.forEach(
        (basePath, dependencies) -> {
          basePath.toFile().mkdirs();
          copyOrCreateSymlinks(basePath, dependencies);

          List<ExternalDependency> filteredDependencies =
              dependencies
                  .stream()
                  .filter(
                      dependency ->
                          !skipPrebuiltDependencyMap.getOrDefault(
                              dependency.getVersionless(), false))
                  .collect(Collectors.toList());
          composeBuckFile(basePath, filteredDependencies);
        });
  }

  private void copyOrCreateSymlinks(Path path, Collection<ExternalDependency> dependencies) {
    SetMultimap<VersionlessDependency, ExternalDependency> nameToDependencyMap =
        MultimapBuilder.hashKeys().hashSetValues().build();
    dependencies.forEach(
        dependency -> nameToDependencyMap.put(dependency.getVersionless(), dependency));

    dependencies.forEach(
        dependency -> {
          FileUtil.symlink(
              path.resolve(dependency.getDependencyFileName()),
              dependency.getRealDependencyFile().toPath());

          Path sourceJar = dependency.getRealSourceFilePath(project);
          if (sourceJar != null) {
            FileUtil.symlink(path.resolve(dependency.getSourceFileName()), sourceJar);
          }
        });
  }

  private void composeBuckFile(Path path, Collection<ExternalDependency> dependencies) {
    List<Rule> rules = PrebuiltRuleComposer.compose(dependencies);
    Multimap<String, String> loadStatements =
        LoadStatementsUtil.getLoadStatements(rules, overridesExtension);
    File buckFile = path.resolve(OkBuckGradlePlugin.BUCK).toFile();
    FileUtil.writeToBuckFile(loadStatements, rules, buckFile);
  }
}
