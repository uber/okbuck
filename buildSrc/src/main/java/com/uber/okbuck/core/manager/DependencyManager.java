package com.uber.okbuck.core.manager;

import static com.uber.okbuck.core.dependency.BaseExternalDependency.AAR;
import static com.uber.okbuck.core.dependency.BaseExternalDependency.JAR;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.common.HttpFileRuleComposer;
import com.uber.okbuck.composer.java.JavaAnnotationProcessorRuleComposer;
import com.uber.okbuck.composer.java.LocalPrebuiltRuleComposer;
import com.uber.okbuck.composer.java.PrebuiltRuleComposer;
import com.uber.okbuck.core.annotation.AnnotationProcessorCache;
import com.uber.okbuck.core.dependency.DependencyFactory;
import com.uber.okbuck.core.dependency.DependencyUtils;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.dependency.LocalExternalDependency;
import com.uber.okbuck.core.dependency.VersionlessDependency;
import com.uber.okbuck.core.model.base.Scope;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyManager {

  private static final Logger LOG = LoggerFactory.getLogger(DependencyManager.class);

  private final Project project;
  private final ExternalDependenciesExtension externalDependenciesExtension;
  private final JetifierExtension jetifierExtension;
  private final BuckFileManager buckFileManager;

  private final Set<org.gradle.api.artifacts.ExternalDependency> rawDependencies = new HashSet<>();

  private final SetMultimap<VersionlessDependency, ExternalDependency> originalDependencyMap =
      LinkedHashMultimap.create();

  private final HashMap<VersionlessDependency, Boolean> skipPrebuiltDependencyMap = new HashMap<>();

  private final HashMap<String, String> sha256Cache;

  public DependencyManager(
      Project rootProject, OkBuckExtension okBuckExtension, BuckFileManager buckFileManager) {

    this.project = rootProject;
    this.externalDependenciesExtension = okBuckExtension.getExternalDependenciesExtension();
    this.jetifierExtension = okBuckExtension.getJetifierExtension();
    this.buckFileManager = buckFileManager;
    this.sha256Cache = initSha256Cache(rootProject);
  }

  public synchronized void addDependencies(
      Set<org.gradle.api.artifacts.ExternalDependency> dependencies) {
    rawDependencies.addAll(dependencies);
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
    updateDependencies(filteredDependencyMap);
    processDependencies(filteredDependencyMap);

    persistSha256Cache(project, sha256Cache);
  }

  private Map<VersionlessDependency, Collection<ExternalDependency>> filterDependencies() {
    if (!externalDependenciesExtension.useLatest()) {
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
              } else if (externalDependenciesExtension.useLatest(key)) {
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
        project, detached, externalDependenciesExtension, jetifierExtension);
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
              .filter(entry -> !externalDependenciesExtension.isVersioned(entry.getKey()))
              .map(Map.Entry::getValue)
              .flatMap(Collection::stream)
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

  private void updateDependencies(
      Map<VersionlessDependency, Collection<ExternalDependency>> dependencyMap) {

    ExternalDependenciesExtension extension = ProjectUtil.getExternalDependencyExtension(project);
    // Don't create exported deps if not enabled.
    if (!extension.exportedDepsEnabled()) {
      return;
    }

    if (!extension.versionlessEnabled()) {
      throw new RuntimeException(
          "Exported deps only works when resolutionAction is latest or single");
    }

    Configuration config = project.getConfigurations().create("okbuckDependencyResolver");
    config.getDependencies().addAll(rawDependencies);

    ResolvedConfiguration resolvedConfiguration = config.getResolvedConfiguration();

    if (resolvedConfiguration.hasError()) {
      // Throw failure if there was one during resolution
      resolvedConfiguration.rethrowFailure();
    }

    resolvedConfiguration
        .getLenientConfiguration()
        .getAllModuleDependencies()
        .forEach(
            rDependency -> {
              Set<ExternalDependency> childDependencies =
                  childDependencies(rDependency, dependencyMap);

              if (childDependencies.size() == 0) {
                return;
              }

              DependencyFactory.fromDependency(rDependency)
                  .stream()
                  .filter(it -> !it.classifier().isPresent())
                  .peek(
                      it -> {
                        if (!dependencyMap.containsKey(it)) {
                          throw dependencyException(rDependency);
                        }
                      })
                  .map(dependencyMap::get)
                  .map(
                      dependencies -> {
                        Preconditions.checkArgument(
                            dependencies.size() == 1,
                            "Dependency having multiple versions can't have deps: " + dependencies);

                        return dependencies.stream().findAny().get();
                      })
                  .forEach(dependency -> dependency.setDeps(childDependencies));
            });
  }

  private static Set<ExternalDependency> childDependencies(
      ResolvedDependency rDependency,
      Map<VersionlessDependency, Collection<ExternalDependency>> dependencyMap) {
    return rDependency
        .getChildren()
        .stream()
        .map(
            cDependency ->
                DependencyFactory.fromDependency(cDependency)
                    .stream()
                    .peek(
                        it -> {
                          if (!dependencyMap.containsKey(it)) {
                            throw dependencyException(cDependency);
                          }
                        })
                    .map(dependencyMap::get)
                    .map(
                        dependencies -> {
                          Preconditions.checkArgument(
                              dependencies.size() == 1,
                              "Child dependencies can't have multiple versions: " + dependencies);

                          return dependencies.stream().findAny().get();
                        })
                    .collect(Collectors.toSet()))
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  private static RuntimeException dependencyException(ResolvedDependency dependency) {
    return new RuntimeException(
        "Couldn't find "
            + dependency
            + " child of parents -> "
            + dependency.getParents()
            + " in final resolved deps.");
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

    AnnotationProcessorCache annotationProcessorCache =
        ProjectUtil.getAnnotationProcessorCache(project);
    Map<Path, List<Scope>> basePathToScopeMap =
        annotationProcessorCache.getBasePathToExternalDependencyScopeMap();

    groupToDependencyMap.forEach(
        (basePath, dependencies) -> {
          ImmutableList.Builder<ExternalDependency> localPrebuiltDependencies =
              ImmutableList.builder();
          ImmutableList.Builder<ExternalDependency> prebuiltDependencies = ImmutableList.builder();
          ImmutableList.Builder<ExternalDependency> httpFileDependencies = ImmutableList.builder();

          if (externalDependenciesExtension.shouldDownloadInBuck()) {
            dependencies.forEach(
                dependency -> {
                  if (dependency instanceof LocalExternalDependency) {
                    localPrebuiltDependencies.add(dependency);
                  } else if (isPrebuiltDependency(dependency)) {
                    prebuiltDependencies.add(dependency);
                  } else {
                    httpFileDependencies.add(dependency);
                  }
                });
          } else {
            localPrebuiltDependencies.addAll(dependencies);
          }

          preComputeSha256(prebuiltDependencies.build(), sha256Cache);
          preComputeSha256(httpFileDependencies.build(), sha256Cache);

          ImmutableList.Builder<Rule> rulesBuilder = ImmutableList.builder();
          rulesBuilder.addAll(LocalPrebuiltRuleComposer.compose(localPrebuiltDependencies.build()));
          rulesBuilder.addAll(
              PrebuiltRuleComposer.compose(prebuiltDependencies.build(), sha256Cache));
          rulesBuilder.addAll(
              HttpFileRuleComposer.compose(httpFileDependencies.build(), sha256Cache));

          // Add annotation processor rules
          List<Scope> scopeList = basePathToScopeMap.get(basePath);
          if (scopeList != null) {
            rulesBuilder.addAll(JavaAnnotationProcessorRuleComposer.compose(scopeList));
          }

          buckFileManager.writeToBuckFile(
              rulesBuilder.build(), basePath.resolve(OkBuckGradlePlugin.BUCK).toFile());

          createSymlinks(basePath, localPrebuiltDependencies.build());
        });
  }

  private boolean isPrebuiltDependency(ExternalDependency dependency) {
    return !skipPrebuiltDependencyMap.getOrDefault(dependency.getVersionless(), false)
        && (dependency.getPackaging().equals(AAR) || dependency.getPackaging().equals(JAR));
  }

  private static void createSymlinks(Path path, Collection<ExternalDependency> dependencies) {
    if (!path.toFile().exists() && !path.toFile().mkdirs()) {
      throw new RuntimeException(String.format("Couldn't create %s when creating symlinks", path));
    }

    SetMultimap<VersionlessDependency, ExternalDependency> nameToDependencyMap =
        MultimapBuilder.hashKeys().hashSetValues().build();
    dependencies.forEach(
        dependency -> nameToDependencyMap.put(dependency.getVersionless(), dependency));

    dependencies.forEach(
        dependency -> {
          FileUtil.symlink(
              path.resolve(dependency.getDependencyFileName()),
              dependency.getRealDependencyFile().toPath());

          dependency
              .getRealSourceFile()
              .ifPresent(
                  file ->
                      FileUtil.symlink(
                          path.resolve(dependency.getSourceFileName()), file.toPath()));
        });
  }

  private static void preComputeSha256(
      List<ExternalDependency> dependencies, HashMap<String, String> sha256Map) {
    dependencies.forEach(
        dependency -> {
          computeSha256IfAbsent(dependency.getRealDependencyFile(), sha256Map);

          Optional<File> sourcesFile = dependency.getRealSourceFile();
          sourcesFile.ifPresent(file -> computeSha256IfAbsent(file, sha256Map));
        });
  }

  private static void computeSha256IfAbsent(File file, HashMap<String, String> sha256Map) {
    String key = ExternalDependency.getGradleSha(file);
    sha256Map.computeIfAbsent(key, k -> DependencyUtils.shaSum256(file));
  }

  private static HashMap<String, String> initSha256Cache(Project rootProject) {
    File projectMappingFile = rootProject.file(OkBuckGradlePlugin.OKBUCK_SHA256);
    try {
      return FileUtil.readMapFromJsonFile(projectMappingFile);
    } catch (IOException e) {
      return new HashMap<>();
    }
  }

  private static void persistSha256Cache(Project rootProject, HashMap<String, String> sha256Map) {
    File projectMappingFile = rootProject.file(OkBuckGradlePlugin.OKBUCK_SHA256);
    try {
      FileUtil.persistMapToJsonFile(sha256Map, projectMappingFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
