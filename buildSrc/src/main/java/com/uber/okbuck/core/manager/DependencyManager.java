package com.uber.okbuck.core.manager;

import com.google.common.base.Joiner;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.java.PrebuiltRuleComposer;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.dependency.VersionlessDependency;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.template.core.Rule;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyManager {

  private static final Logger LOG = LoggerFactory.getLogger(DependencyManager.class);

  private final Project project;
  private final String publishDirName;
  private final Map<VersionlessDependency, List<String>> allowed;

  public DependencyManager(
      Project rootProject,
      String publishDirName,
      Map<VersionlessDependency, List<String>> allowed) {
    this.project = rootProject;
    this.publishDirName = publishDirName;
    this.allowed = allowed;
  }

  private static SetMultimap<VersionlessDependency, ExternalDependency> dependencyMap =
      Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build());

  public void addDependency(ExternalDependency dependency) {
    dependencyMap.put(dependency.versionless, dependency);
  }

  public void finalizeDependencies() {
    validateDependencies();
    processDependencies();
  }

  private void validateDependencies() {
    Map<String, Set<String>> errors =
        dependencyMap
            .asMap()
            .entrySet()
            .stream()
            .map(
                entry -> {
                  VersionlessDependency key = entry.getKey();
                  Collection<ExternalDependency> value = entry.getValue();

                  if (value.size() > 1) {
                    if (allowed.containsKey(key)) {
                      List<String> allowedVersions = allowed.get(key);
                      if (allowedVersions.size() != 0) {
                        List<ExternalDependency> extraDependencies =
                            value
                                .stream()
                                .filter(dependency -> !allowedVersions.contains(dependency.version))
                                .collect(Collectors.toList());

                        if (extraDependencies.size() > 0) {
                          return extraDependencies;
                        }
                      } else {
                        // All versions are allowed -- continue.
                      }
                    } else {
                      return value;
                    }
                  }
                  return null;
                })
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .collect(
                Collectors.groupingBy(
                    dependency -> dependency.versionless.toString(),
                    Collectors.mapping(i -> i.version, Collectors.toSet())));

    if (errors.size() > 0) {
      Joiner.MapJoiner mapJoiner = Joiner.on(",\n").withKeyValueSeparator("=");
      throw new RuntimeException(
          "Extra versions found for external dependencies  \n" + mapJoiner.join(errors));
    }
  }

  private void processDependencies() {
    File publishDir = project.file(publishDirName);
    if (publishDir.exists()) {
      try {
        FileUtils.deleteDirectory(publishDir);
      } catch (IOException e) {
        throw new RuntimeException("Could not delete dependency directory: " + publishDir);
      }
    }

    if (!publishDir.mkdirs()) {
      throw new IllegalStateException("Couldn't create dependency directory: " + publishDir);
    }

    Path basePath = project.getProjectDir().toPath();

    dependencyMap
        .asMap()
        .values()
        .stream()
        .flatMap(Collection::stream)
        .collect(Collectors.groupingBy(ExternalDependency::getGroup))
        .forEach(
            (group, dependencies) -> {
              Path groupDirPath =
                  basePath.resolve(publishDirName).resolve(group.replace('.', File.separatorChar));

              groupDirPath.toFile().mkdirs();
              copyOrCreateSymlinks(groupDirPath, dependencies);
              composeBuckFile(groupDirPath, dependencies);
            });
  }

  private void copyOrCreateSymlinks(Path path, Collection<ExternalDependency> dependencies) {
    SetMultimap<VersionlessDependency, ExternalDependency> nameToDependencyMap =
        MultimapBuilder.hashKeys().hashSetValues().build();
    dependencies.forEach(
        dependency -> {
          nameToDependencyMap.put(dependency.versionless, dependency);
        });

    dependencies.forEach(
        dependency -> {
          symlink(path.resolve(dependency.getDepFileName()), dependency.depFile.toPath());

          Path sourceJar = dependency.getSourceJar(project);
          if (sourceJar != null) {
            symlink(path.resolve(dependency.getSourceFileName()), sourceJar);
          }

          Path lintJar = dependency.getLintJar();
          if (lintJar != null) {
            try {
              Files.copy(
                  lintJar,
                  path.resolve(dependency.getLintFileName()),
                  StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        });
  }

  private void symlink(Path link, Path target) {
    try {
      LOG.info("Creating symlink {} -> {}", link, target);
      Files.createSymbolicLink(link, target);
    } catch (IOException e) {
      LOG.error("Could not create symlink {} -> {}", link, target);
      throw new IllegalStateException(e);
    }
  }

  private void composeBuckFile(Path path, Collection<ExternalDependency> dependencies) {
    List<Rule> rules = PrebuiltRuleComposer.compose(dependencies);
    File buckFile = path.resolve(OkBuckGradlePlugin.BUCK).toFile();
    FileUtil.writeToBuckFile(rules, buckFile);
  }
}
