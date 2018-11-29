package com.uber.okbuck.core.manager;

import static com.uber.okbuck.core.dependency.BaseExternalDependency.AAR;
import static com.uber.okbuck.core.dependency.BaseExternalDependency.JAR;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.common.HttpFileRuleComposer;
import com.uber.okbuck.composer.java.LocalPrebuiltRuleComposer;
import com.uber.okbuck.composer.java.PrebuiltRuleComposer;
import com.uber.okbuck.core.dependency.DependencyUtils;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.dependency.LocalExternalDependency;
import com.uber.okbuck.core.dependency.VersionlessDependency;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.ExternalDependenciesExtension;
import com.uber.okbuck.extension.JetifierExtension;
import com.uber.okbuck.extension.OkBuckExtension;
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
  private final ExternalDependenciesExtension externalDependenciesExtension;
  private final JetifierExtension jetifierExtension;
  private final BuckFileManager buckFileManager;

  private final SetMultimap<VersionlessDependency, ExternalDependency> originalDependencyMap =
      LinkedHashMultimap.create();

  private final HashMap<VersionlessDependency, Boolean> skipPrebuiltDependencyMap = new HashMap<>();

  public DependencyManager(
      Project rootProject, OkBuckExtension okBuckExtension, BuckFileManager buckFileManager) {

    this.project = rootProject;
    this.externalDependenciesExtension = okBuckExtension.getExternalDependenciesExtension();
    this.jetifierExtension = okBuckExtension.getJetifierExtension();
    this.buckFileManager = buckFileManager;
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

  public void finalizeDependencies() {
    Map<VersionlessDependency, Collection<ExternalDependency>> filteredDependencyMap =
        filterDependencies();
    validateDependencies(filteredDependencyMap);
    processDependencies(filteredDependencyMap);
  }

  private Map<VersionlessDependency, Collection<ExternalDependency>> filterDependencies() {
    if (!externalDependenciesExtension.allowLatestEnabled()) {
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
              } else if (externalDependenciesExtension.isAllowLatestFor(key)) {
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
    return DependencyUtils.resolveExternal(
        detached, externalDependenciesExtension, jetifierExtension);
  }

  private void validateDependencies(
      Map<VersionlessDependency, Collection<ExternalDependency>> dependencyMap) {
    if (externalDependenciesExtension.versionlessEnabled()) {
      Joiner.MapJoiner mapJoiner = Joiner.on(",\n").withKeyValueSeparator("=");

      Map<String, Set<String>> extraDependencies =
          dependencyMap
              .entrySet()
              .stream()
              .filter(entry -> entry.getValue().size() > 1)
              .map(Map.Entry::getValue)
              .flatMap(Collection::stream)
              .filter(dependency -> !externalDependenciesExtension.isAllowedVersion(dependency))
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
              .filter(
                  dependency ->
                      externalDependenciesExtension.isVersioned(dependency.getVersionless()))
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
            .map(ExternalDependency::getTargetName)
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
    Path rootPath = project.getRootDir().toPath();
    File cacheDir = rootPath.resolve(externalDependenciesExtension.getCache()).toFile();
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
                Collectors.groupingBy(dependency -> rootPath.resolve(dependency.getTargetPath())));

    groupToDependencyMap.forEach(
        (basePath, dependencies) -> {
          ImmutableList.Builder<LocalExternalDependency> localPrebuiltDependencies =
              ImmutableList.builder();
          ImmutableList.Builder<ExternalDependency> prebuiltDependencies = ImmutableList.builder();
          ImmutableList.Builder<ExternalDependency> httpFileDependencies = ImmutableList.builder();

          dependencies.forEach(
              dependency -> {
                if (dependency instanceof LocalExternalDependency) {
                  localPrebuiltDependencies.add((LocalExternalDependency) dependency);
                } else if (isPrebuiltDependency(dependency)) {
                  prebuiltDependencies.add(dependency);
                } else {
                  httpFileDependencies.add(dependency);
                }
              });

          ImmutableList.Builder<Rule> rulesBuilder = ImmutableList.builder();
          rulesBuilder.addAll(LocalPrebuiltRuleComposer.compose(localPrebuiltDependencies.build()));
          rulesBuilder.addAll(PrebuiltRuleComposer.compose(prebuiltDependencies.build()));
          rulesBuilder.addAll(HttpFileRuleComposer.compose(httpFileDependencies.build()));

          buckFileManager.writeToBuckFile(
              rulesBuilder.build(), basePath.resolve(OkBuckGradlePlugin.BUCK).toFile());

          copyOrCreateSymlinks(basePath, localPrebuiltDependencies.build());
        });
  }

  private boolean isPrebuiltDependency(ExternalDependency dependency) {
    return !skipPrebuiltDependencyMap.getOrDefault(dependency.getVersionless(), false)
        && (dependency.getPackaging().equals(AAR) || dependency.getPackaging().equals(JAR));
  }

  private static void copyOrCreateSymlinks(
      Path path, Collection<LocalExternalDependency> dependencies) {
    path.toFile().mkdirs();

    SetMultimap<VersionlessDependency, ExternalDependency> nameToDependencyMap =
        MultimapBuilder.hashKeys().hashSetValues().build();
    dependencies.forEach(
        dependency -> nameToDependencyMap.put(dependency.getVersionless(), dependency));

    dependencies.forEach(
        dependency -> {
          FileUtil.symlink(
              path.resolve(dependency.getDependencyFileName()),
              dependency.getRealDependencyFile().toPath());

          File sourceJar = dependency.getRealSourceFile();
          if (sourceJar != null) {
            FileUtil.symlink(path.resolve(dependency.getSourceFileName()), sourceJar.toPath());
          }
        });
  }
}
