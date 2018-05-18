package com.uber.okbuck.core.dependency;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.base.Store;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.artifacts.ivyservice.DefaultLenientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyCache {

  private static final Logger LOG = LoggerFactory.getLogger(DependencyCache.class);
  private final File cacheDir;
  private final Project rootProject;
  private final boolean fetchSources;
  private final Store lintJars;
  private final Store processors;
  private final Store processorExtensions;
  private final Store sources;
  private final Set<File> copies = ConcurrentHashMap.newKeySet();
  private final Set<ExternalDependency> requested = ConcurrentHashMap.newKeySet();
  private final Map<File, File> links = new ConcurrentHashMap<>();
  private final Map<ExternalDependency.VersionlessDependency, ExternalDependency> forcedDeps =
      new HashMap<>();

  public DependencyCache(Project project, File cacheDir, @Nullable String forcedConfiguration) {
    this.rootProject = project.getRootProject();
    this.cacheDir = cacheDir;
    this.fetchSources = ProjectUtil.getOkBuckExtension(project).getIntellijExtension().sources;

    sources = new Store(rootProject.file(OkBuckGradlePlugin.OKBUCK_STATE_DIR + "/SOURCES"));
    processors = new Store(rootProject.file(OkBuckGradlePlugin.OKBUCK_STATE_DIR + "/PROCESSORS"));
    processorExtensions =
        new Store(rootProject.file(OkBuckGradlePlugin.OKBUCK_STATE_DIR + "/PROCESSORS_EXTENSIONS"));
    lintJars = new Store(rootProject.file(OkBuckGradlePlugin.OKBUCK_STATE_DIR + "/LINT_JARS"));

    if (forcedConfiguration != null) {
      Scope.from(project, forcedConfiguration)
          .getExternal()
          .forEach(
              dependency -> {
                get(dependency);
                forcedDeps.put(dependency.versionless, dependency);
              });
    }
  }

  public DependencyCache(Project project, File cacheDir) {
    this(project, cacheDir, null);
  }

  public void finalizeDeps() {
    LOG.info("Finalizing Dependency Cache");
    sources.persist();
    processors.persist();
    lintJars.persist();
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

    File[] stale =
        cacheDir.listFiles(
            pathname ->
                pathname.isFile()
                    && !links.keySet().contains(pathname)
                    && !copies.contains(pathname)
                    && (pathname.getName().endsWith(".jar")
                        || pathname.getName().endsWith(".aar")
                        || pathname.getName().endsWith(".pro")
                        || pathname.getName().endsWith(".pex")));

    if (stale != null) {
      Arrays.asList(stale)
          .forEach(
              file -> {
                try {
                  Files.deleteIfExists(file.toPath());
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }

    links.forEach(
        (link, target) -> {
          if (link.exists()) {
            return;
          }
          try {
            LOG.info("Creating symlink {} -> {}", link, target);
            Files.createSymbolicLink(link.toPath(), target.toPath());
          } catch (IOException ignored) {
            LOG.info("Could not create symlink {} -> {}", link, target);
          }
        });
  }

  public String get(
      ExternalDependency externalDependency, boolean resolveOnly, boolean useFullDepName) {
    LOG.info("Requested dependency {}", externalDependency);
    ExternalDependency dependency =
        forcedDeps.getOrDefault(externalDependency.versionless, externalDependency);
    LOG.info("Picked dependency {}", dependency);

    File cachedCopy = new File(cacheDir, dependency.getCacheName(useFullDepName));
    String key = FileUtil.getRelativePath(rootProject.getProjectDir(), cachedCopy);
    links.put(cachedCopy, dependency.depFile);

    if (!resolveOnly && fetchSources) {
      LOG.info("Fetching sources for {}", dependency);
      getSources(dependency);
    }

    requested.add(dependency);

    return key;
  }

  public String get(ExternalDependency externalDependency, boolean resolveOnly) {
    return get(externalDependency, resolveOnly, true);
  }

  public String get(ExternalDependency externalDependency) {
    return get(externalDependency, false, true);
  }

  /**
   * Gets the sources jar path for a dependency if it exists.
   *
   * @param dependency The External dependency.
   */
  void getSources(ExternalDependency dependency) {
    String key = dependency.getCacheName();
    String sourcesJarPath = sources.get(key);

    if (sourcesJarPath == null || !Files.exists(Paths.get(sourcesJarPath))) {
      sourcesJarPath = "";
      if (!DependencyUtils.isWhiteListed(dependency.depFile)) {
        String sourcesJarName = dependency.getSourceCacheName(false);
        File sourcesJar = new File(dependency.depFile.getParentFile(), sourcesJarName);

        if (!Files.exists(sourcesJar.toPath())) {
          if (!dependency.isLocal) {
            // Most likely jar is in Gradle/Maven cache directory, try to find sources jar in
            // "jar/../..".
            FileTree sourceJars =
                rootProject.fileTree(
                    ImmutableMap.of(
                        "dir", dependency.depFile.getParentFile().getParentFile().getAbsolutePath(),
                        "includes", ImmutableList.of("**/" + sourcesJarName)));

            try {
              sourcesJarPath = sourceJars.getSingleFile().getAbsolutePath();
            } catch (IllegalStateException ignored) {
              if (sourceJars.getFiles().size() > 1) {
                throw new IllegalStateException(
                    "Found multiple source jars: " + sourceJars + " for " + dependency);
              }
            }
          }
        }
      }
      sources.set(key, sourcesJarPath);
    }

    if (!sourcesJarPath.isEmpty()) {
      links.put(new File(cacheDir, dependency.getSourceCacheName(true)), new File(sourcesJarPath));
    }
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
   * @param externalDependency The depenency
   * @return path to the lint jar in the cache.
   */
  @Nullable
  public String getLintJar(ExternalDependency externalDependency) {
    ExternalDependency dependency =
        forcedDeps.getOrDefault(externalDependency.versionless, externalDependency);
    return getAarEntry(dependency, lintJars, "lint.jar", "-lint.jar");
  }

  public void build(Configuration configuration, boolean cleanupDeps, boolean useFullDepname) {
    build(Collections.singleton(configuration), cleanupDeps, useFullDepname);
  }

  public void build(Configuration configuration, boolean cleanupDeps) {
    build(configuration, cleanupDeps, false);
  }

  public void build(Configuration configuration) {
    build(configuration, true, false);
  }

  /**
   * Use this method to populate dependency caches of tools/languages etc. This is not meant to be
   * used across multiple threads/gradle task executions which can run in parallel. This method is
   * fully synchronous.
   *
   * @param configurations The set of configurations to materialize into the dependency cache
   */
  void build(Set<Configuration> configurations, boolean cleanupDeps, boolean useFullDepname) {
    configurations.forEach(
        configuration -> {
          try {
            configuration
                .getIncoming()
                .getArtifacts()
                .getArtifacts()
                .forEach(
                    artifact -> {
                      ComponentIdentifier identifier = artifact.getId().getComponentIdentifier();
                      if (identifier instanceof ProjectComponentIdentifier
                          || !DependencyUtils.isConsumable(artifact.getFile())) {
                        return;
                      }
                      ExternalDependency dependency;
                      if (identifier instanceof ModuleComponentIdentifier
                          && ((ModuleComponentIdentifier) identifier).getVersion().length() > 0) {
                        ModuleComponentIdentifier moduleIdentifier =
                            (ModuleComponentIdentifier) identifier;
                        dependency =
                            new ExternalDependency(
                                moduleIdentifier.getGroup(),
                                moduleIdentifier.getModule(),
                                moduleIdentifier.getVersion(),
                                artifact.getFile());
                      } else {
                        dependency = ExternalDependency.fromLocal(artifact.getFile());
                      }
                      get(dependency, true, useFullDepname);
                    });
          } catch (DefaultLenientConfiguration.ArtifactResolveException e) {
            throw new IllegalStateException(
                "Failed to resolve an artifact. Make sure you have a repositories block defined. "
                    + "See https://github.com/uber/okbuck/wiki/Known-caveats#could-not-resolve-all-dependencies-for-configuration for more information.",
                e);
          }
        });

    if (cleanupDeps) {
      cleanup();
    }
  }

  /**
   * Use this method to populate dependency caches of tools/languages etc. This is not meant to be
   * used across multiple threads/gradle task executions which can run in parallel. This method is
   * fully synchronous.
   *
   * @param configurations The set of configurations to materialize into the dependency cache
   */
  public void build(Set<Configuration> configurations, boolean cleanupDeps) {
    build(configurations, cleanupDeps, false);
  }

  /**
   * Use this method to populate dependency caches of tools/languages etc. This is not meant to be
   * used across multiple threads/gradle task executions which can run in parallel. This method is
   * fully synchronous.
   *
   * @param configurations The set of configurations to materialize into the dependency cache
   */
  public void build(Set<Configuration> configurations) {
    build(configurations, true, false);
  }

  @Nullable
  private String getAarEntry(
      ExternalDependency dependency, Store store, String entry, String suffix) {
    if (!dependency.depFile.getName().endsWith(".aar")) {
      return null;
    }

    String key = dependency.getCacheName();
    String entryPath = store.get(key);
    if (entryPath == null || !Files.exists(Paths.get(entryPath))) {
      entryPath = "";
      try {
        File packagedEntry =
            getPackagedFile(dependency.depFile, new File(cacheDir, key), entry, suffix);
        if (packagedEntry != null) {
          entryPath = FileUtil.getRelativePath(rootProject.getProjectDir(), packagedEntry);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      store.set(key, entryPath);
    }

    if (!entryPath.isEmpty()) {
      copies.add(new File(rootProject.getProjectDir(), entryPath));
    }

    return entryPath;
  }

  @Nullable
  private static File getPackagedFile(File aar, File destination, String entry, String suffix)
      throws IOException {
    File packagedFile =
        new File(
            destination.getParentFile(), destination.getName().replaceFirst("\\.aar$", suffix));
    if (Files.exists(packagedFile.toPath())) {
      return packagedFile;
    }

    FileSystem zipFile = FileSystems.newFileSystem(aar.toPath(), null);
    Path packagedPath = zipFile.getPath(entry);
    if (Files.exists(packagedPath)) {
      try {
        Files.copy(packagedPath, packagedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException ignored) {
        LOG.info("Could not create copy {}", packagedFile);
      }
      return packagedFile;
    } else {
      return null;
    }
  }
}
