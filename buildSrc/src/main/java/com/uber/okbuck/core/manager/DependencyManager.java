package com.uber.okbuck.core.manager;

import static com.uber.okbuck.core.dependency.OResolvedDependency.AAR;
import static com.uber.okbuck.core.dependency.OResolvedDependency.JAR;

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
import com.uber.okbuck.core.dependency.LocalOExternalDependency;
import com.uber.okbuck.core.dependency.OExternalDependency;
import com.uber.okbuck.core.dependency.VersionlessDependency;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectCache;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.ExternalDependenciesExtension;
import com.uber.okbuck.extension.JetifierExtension;
import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.template.core.Rule;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalDependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;

public class DependencyManager {

  private final Project project;
  private final ExternalDependenciesExtension externalDependenciesExtension;
  private final JetifierExtension jetifierExtension;
  private final BuckFileManager buckFileManager;

  private final Set<ExternalDependency> rawDependencies = new HashSet<>();

  private final SetMultimap<VersionlessDependency, OExternalDependency> originalDependencyMap =
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

  public synchronized void addRawDependencies(Set<ExternalDependency> dependencies) {
    rawDependencies.addAll(dependencies);
  }

  public synchronized void addDependency(OExternalDependency dependency, boolean skipPrebuilt) {
    VersionlessDependency versionless = dependency.getVersionless();
    originalDependencyMap.put(versionless, dependency);

    if (skipPrebuiltDependencyMap.containsKey(versionless)) {
      skipPrebuiltDependencyMap.put(
          versionless, skipPrebuiltDependencyMap.get(versionless) && skipPrebuilt);
    } else {
      skipPrebuiltDependencyMap.put(versionless, skipPrebuilt);
    }
  }

  public void resolveCurrentRawDeps() {
    if (!externalDependenciesExtension.resoleOnlyThirdParty()) {
      return;
    }

    Map<String, List<ExternalDependency>> rawDepsMap =
        rawDependencies
            .stream()
            .collect(Collectors.groupingBy(i -> i.getGroup() + ":" + i.getVersion()));

    List<Project> allProjects = new ArrayList<>(project.getAllprojects());
    int numberOfChunks = allProjects.size();

    List<Map<String, List<ExternalDependency>>> chunksRawDepsMap =
        rawDepsMap
            .keySet()
            .stream()
            .collect(Collectors.groupingBy(key -> Math.abs(key.hashCode()) % numberOfChunks))
            .values()
            .stream()
            .map(chunk -> chunk.stream().collect(Collectors.toMap(key -> key, rawDepsMap::get)))
            .collect(Collectors.toList());

    IntStream.range(0, allProjects.size())
        .parallel()
        .forEach(
            i -> {
              resolveDepsWithProject(allProjects.get(i), chunksRawDepsMap.get(i));
            });
  }

  private static void resolveDepsWithProject(
      Project project, Map<String, List<ExternalDependency>> depsMap) {
    if (project != project.getRootProject()) {
      ProjectCache.initScopeCache(project);
    }

    for (Map.Entry<String, List<ExternalDependency>> e : depsMap.entrySet()) {
      Configuration config =
          project
              .getConfigurations()
              .maybeCreate("resolve__" + e.getKey().replace(".", "__").replace(":", "__"));
      config.getDependencies().addAll(e.getValue());
      Scope.builder(project).configuration(config).build();
    }

    if (project != project.getRootProject()) {
      ProjectCache.resetScopeCache(project);
    }
  }

  public void finalizeDependencies() {
    Map<VersionlessDependency, Collection<OExternalDependency>> filteredDependencyMap =
        filterDependencies();

    validateDependencies(filteredDependencyMap);
    updateDependencies(filteredDependencyMap);
    processDependencies(filteredDependencyMap);

    persistSha256Cache(project, sha256Cache);
  }

  private Map<VersionlessDependency, Collection<OExternalDependency>> filterDependencies() {
    Map<VersionlessDependency, Collection<OExternalDependency>> dependencies =
        originalDependencyMap.asMap();

    // Update first level of all versions of a dep if any one version has first level as true

    if (externalDependenciesExtension.shouldMarkFirstLevelAllVersions()) {
      dependencies
          .values()
          .forEach(
              value -> {
                boolean firstLevel = value.stream().anyMatch(OExternalDependency::isFirstLevel);
                value.forEach(
                    externalDependency -> externalDependency.updateFirstLevel(firstLevel));
              });
    }

    if (!externalDependenciesExtension.useLatest()) {
      return dependencies;
    }

    ImmutableMap.Builder<VersionlessDependency, Collection<OExternalDependency>>
        filteredDependencyMapBuilder = ImmutableMap.builder();

    ImmutableList.Builder<OExternalDependency> dependenciesToResolveBuilder =
        ImmutableList.builder();

    dependencies.forEach(
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

  private Set<OExternalDependency> resolved(Collection<OExternalDependency> externalDependencies) {
    Configuration detached =
        project
            .getConfigurations()
            .detachedConfiguration(
                externalDependencies
                    .stream()
                    .map(OExternalDependency::getAsGradleDependency)
                    .toArray(Dependency[]::new));
    return DependencyUtils.resolveExternal(
        project, detached, externalDependenciesExtension, jetifierExtension);
  }

  private void validateDependencies(
      Map<VersionlessDependency, Collection<OExternalDependency>> dependencyMap) {
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
                      Collectors.mapping(OExternalDependency::getVersion, Collectors.toSet())));

      if (extraDependencies.size() > 0) {
        throw new RuntimeException(
            "Multiple versions found for external dependencies: \n"
                + mapJoiner.join(extraDependencies));
      }

      Map<String, Set<String>> singleDependencies =
          dependencyMap
              .values()
              .stream()
              .filter(externalDependencies -> externalDependencies.size() == 1)
              .flatMap(Collection::stream)
              .filter(
                  dependency ->
                      externalDependenciesExtension.isVersioned(dependency.getVersionless()))
              .collect(
                  Collectors.groupingBy(
                      dependency -> dependency.getVersionless().mavenCoords(),
                      Collectors.mapping(OExternalDependency::getVersion, Collectors.toSet())));

      if (singleDependencies.size() > 0) {
        throw new RuntimeException(
            "Single version found for external dependencies, please remove them from external dependency extension: \n"
                + mapJoiner.join(singleDependencies));
      }
    }
  }

  private void updateDependencies(
      Map<VersionlessDependency, Collection<OExternalDependency>> dependencyMap) {

    ExternalDependenciesExtension extension = ProjectUtil.getExternalDependencyExtension(project);
    // This code-path is when versionless & exported deps is enabled
    if (!extension.versionlessExportedDepsEnabled()) {
      return;
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
              List<Collection<OExternalDependency>> oExternal =
                  DependencyFactory.fromDependency(rDependency)
                      .stream()
                      .peek(
                          it -> {
                            if (!dependencyMap.containsKey(it)) {
                              dependencyException(rDependency);
                            }
                          })
                      .map(dependencyMap::get)
                      .collect(Collectors.toList());

              // Is a firstLevel dependency if it has a parent with no parents.
              boolean firstLevel =
                  rDependency
                      .getParents()
                      .stream()
                      .anyMatch(parent -> parent.getParents().size() == 0);

              // Update first level state.
              oExternal
                  .stream()
                  .flatMap(Collection::stream)
                  .forEach(external -> external.updateFirstLevel(firstLevel));

              Set<OExternalDependency> childDependencies =
                  childDependencies(rDependency, dependencyMap);
              if (childDependencies.size() == 0) {
                return;
              }

              oExternal
                  .stream()
                  .map(
                      dependencies -> {
                        Preconditions.checkArgument(
                            dependencies.size() == 1,
                            "Dependency having multiple versions can't have deps: m"
                                + dependencies);

                        return dependencies.stream().findAny().get();
                      })
                  .forEach(
                      dependency -> {
                        dependency.addDeps(childDependencies);
                      });
            });
  }

  private static Set<OExternalDependency> childDependencies(
      ResolvedDependency rDependency,
      Map<VersionlessDependency, Collection<OExternalDependency>> dependencyMap) {
    return rDependency
        .getChildren()
        .stream()
        .map(
            cDependency ->
                // TODO:Replace with using childFromDependency instead
                // which ensures that the right child deps are only fetched.
                DependencyFactory.fromDependency(cDependency)
                    .stream()
                    .peek(
                        it -> {
                          if (!dependencyMap.containsKey(it)) {
                            dependencyException(cDependency);
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

  private static void dependencyException(ResolvedDependency dependency) {
    throw new RuntimeException(
        "Couldn't find "
            + dependency
            + " child of parents -> "
            + dependency.getParents()
            + " in final resolved deps.");
  }

  private void processDependencies(
      Map<VersionlessDependency, Collection<OExternalDependency>> dependencyMap) {
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

    Map<Path, List<OExternalDependency>> groupToDependencyMap =
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
          ImmutableList.Builder<OExternalDependency> localPrebuiltDependencies =
              ImmutableList.builder();
          ImmutableList.Builder<OExternalDependency> prebuiltDependencies = ImmutableList.builder();
          ImmutableList.Builder<OExternalDependency> httpFileDependencies = ImmutableList.builder();

          if (externalDependenciesExtension.shouldDownloadInBuck()) {
            dependencies.forEach(
                dependency -> {
                  if (dependency instanceof LocalOExternalDependency) {
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

  private boolean isPrebuiltDependency(OExternalDependency dependency) {
    return !skipPrebuiltDependencyMap.getOrDefault(dependency.getVersionless(), false)
        && (dependency.getPackaging().equals(AAR) || dependency.getPackaging().equals(JAR));
  }

  private static void createSymlinks(Path path, Collection<OExternalDependency> dependencies) {
    if (!path.toFile().exists() && !path.toFile().mkdirs()) {
      throw new RuntimeException(String.format("Couldn't create %s when creating symlinks", path));
    }

    SetMultimap<VersionlessDependency, OExternalDependency> nameToDependencyMap =
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
      List<OExternalDependency> dependencies, HashMap<String, String> sha256Map) {
    dependencies.forEach(
        dependency -> {
          computeSha256IfAbsent(dependency.getRealDependencyFile(), sha256Map);

          Optional<File> sourcesFile = dependency.getRealSourceFile();
          sourcesFile.ifPresent(file -> computeSha256IfAbsent(file, sha256Map));
        });
  }

  private static void computeSha256IfAbsent(File file, HashMap<String, String> sha256Map) {
    String key = OExternalDependency.getGradleSha(file);
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
