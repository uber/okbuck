package com.uber.okbuck.core.dependency

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.base.Store
import com.uber.okbuck.core.util.FileUtil
import org.apache.commons.io.IOUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.jar.JarEntry
import java.util.jar.JarFile

class DependencyCache {

    private final File cacheDir
    private final Project rootProject
    private final boolean fetchSources
    private final Store lintJars
    private final Store processors
    private final Store proguardConfigs
    private final Store sources

    private final Set<File> copies = new HashSet<>()
    private final Set<ExternalDependency> requested = new HashSet<>()
    private final Map<File, File> links = new HashMap<>()
    private final Map<ExternalDependency.VersionlessDependency, ExternalDependency> forcedDeps = new HashMap<>()

    DependencyCache(Project project, File cacheDir, String forcedConfiguration = null) {
        this.rootProject = project.rootProject
        this.cacheDir = cacheDir
        this.fetchSources = rootProject.okbuck.intellij.sources

        sources = new Store(new File("${OkBuckGradlePlugin.OKBUCK_STATE_DIR}/SOURCES"))
        processors = new Store(new File("${OkBuckGradlePlugin.OKBUCK_STATE_DIR}/PROCESSORS"))
        lintJars = new Store(new File("${OkBuckGradlePlugin.OKBUCK_STATE_DIR}/LINT_JARS"))
        proguardConfigs = new Store(new File("${OkBuckGradlePlugin.OKBUCK_STATE_DIR}/PROGUARD_CONFIGS"))

        if (forcedConfiguration) {
            Scope.from(project, Collections.singleton(forcedConfiguration)).external.each {
                get(it)
                forcedDeps.put(it.versionless, it)
            }
        }
    }

    void finalizeDeps() {
        sources.persist()
        processors.persist()
        lintJars.persist()
        proguardConfigs.persist()
        cleanup()
    }

    void cleanup() {
        requested.each { ExternalDependency dependency ->
            DependencyUtils.validate(rootProject, dependency)
        }

        Set<File> existing = cacheDir.listFiles(new FileFilter() {
            @Override
            boolean accept(File pathname) {
                return pathname.isFile() && (pathname.name.endsWith(".jar")
                        || pathname.name.endsWith(".aar")
                        || pathname.name.endsWith(".pro"))
            }
        })

        Set<File> toCreate = links.keySet()

        (existing - (toCreate + copies)).each { Files.deleteIfExists(it.toPath()) }

        links.keySet().removeAll(existing)
        links.each { File link, File target ->
            if (target.exists()) {
                try {
                    Files.createSymbolicLink(link.toPath(), target.toPath())
                } catch (IOException ignored) { }
            }
        }
    }

    String get(ExternalDependency externalDependency, boolean resolveOnly = false, boolean useFullDepname = true) {
        ExternalDependency dependency = forcedDeps.getOrDefault(externalDependency.versionless, externalDependency)
        File cachedCopy = new File(cacheDir, dependency.getCacheName(useFullDepname))
        String key = FileUtil.getRelativePath(rootProject.projectDir, cachedCopy)
        links.put(cachedCopy, dependency.depFile)

        if (!resolveOnly && fetchSources) {
            getSources(dependency)
        }
        requested.add(dependency)

        return key
    }

    /**
     * Gets the sources jar path for a dependency if it exists.
     *
     * @param externalDependency The dependency.
     */
    void getSources(ExternalDependency externalDependency) {
        ExternalDependency dependency = forcedDeps.getOrDefault(externalDependency.versionless, externalDependency)
        String key = dependency.cacheName
        String sourcesJarPath = sources.get(key)
        if (sourcesJarPath == null || !Files.exists(Paths.get(sourcesJarPath))) {
            sourcesJarPath = ""
            if (!DependencyUtils.isWhiteListed(dependency.depFile)) {
                String sourcesJarName = dependency.getSourceCacheName(false)
                File sourcesJar = new File(dependency.depFile.parentFile, sourcesJarName)

                if (!Files.exists(sourcesJar.toPath())) {
                    if (!dependency.isLocal) {
                        // Most likely jar is in Gradle/Maven cache directory, try to find sources jar in "jar/../..".
                        def sourceJars = rootProject.fileTree(
                                dir: dependency.depFile.parentFile.parentFile.absolutePath,
                                includes: ["**/${sourcesJarName}"]) as List

                        if (sourceJars.size() == 1) {
                            sourcesJarPath = sourceJars[0].absolutePath
                        } else if (sourceJars.size() > 1) {
                            throw new IllegalStateException("Found multiple source jars: ${sourceJars} for ${dependency}")
                        }
                    }
                }
            }
            sources.set(key, sourcesJarPath)
        }
        if (sourcesJarPath) {
            links.put(new File(cacheDir, dependency.getSourceCacheName(true)), new File(sourcesJarPath))
        }
    }

    /**
     * Get the list of annotation processor classes provided by a dependency.
     *
     * @param externalDependency The dependency
     * @return The list of annotation processor classes available in the manifest
     */
    List<String> getAnnotationProcessors(ExternalDependency externalDependency) {
        ExternalDependency dependency = forcedDeps.getOrDefault(externalDependency.versionless, externalDependency)
        String key = dependency.cacheName
        String processorsList = processors.get(key)
        if (processorsList == null) {
            JarFile jarFile = new JarFile(dependency.depFile)
            JarEntry jarEntry = (JarEntry) jarFile.getEntry("META-INF/services/javax.annotation.processing.Processor")
            if (jarEntry) {
                List<String> processorClasses = IOUtils.toString(jarFile.getInputStream(jarEntry))
                        .trim().split("\\n").findAll { String entry ->
                    !entry.startsWith('#') && !entry.trim().empty // filter out comments and empty lines
                }
                processorsList = processorClasses.join(",")
            } else {
                processorsList = ""
            }
            processors.set(key, processorsList)
        }

        if (processorsList == "") {
            return Collections.emptyList()
        } else {
            return processorsList.split(",")
        }
    }

    /**
     * Get the packaged lint jar of an aar dependency if any.
     *
     * @param externalDependency The depenency
     * @return path to the lint jar in the cache.
     */
    String getLintJar(ExternalDependency externalDependency) {
        ExternalDependency dependency = forcedDeps.getOrDefault(externalDependency.versionless, externalDependency)
        return getAarEntry(dependency, lintJars, "lint.jar", "-lint.jar")
    }

    /**
     * Get the packaged proguard config of an aar dependency if any.
     *
     * @param externalDependency The depenency
     * @return path to the proguard config in the cache.
     */
    File getProguardConfig(ExternalDependency externalDependency) {
        ExternalDependency dependency = forcedDeps.getOrDefault(externalDependency.versionless, externalDependency)
        String entry = getAarEntry(dependency, proguardConfigs, "proguard.txt", "-proguard.pro")
        if (entry) {
            return new File(entry)
        } else {
            return null
        }
    }

    void build(Configuration configuration, boolean cleanupDeps = true, boolean useFullDepname = false) {
        build(Collections.singleton(configuration), cleanupDeps, useFullDepname)
    }

    /**
     * Use this method to populate dependency caches of tools/languages etc. This is not meant to be used across
     * multiple threads/gradle task executions which can run in parallel. This method is fully synchronous.
     *
     * @param configurations The set of configurations to materialize into the dependency cache
     */
    void build(Set<Configuration> configurations, boolean cleanupDeps = true, boolean useFullDepname = false) {
        configurations.each { Configuration configuration ->
            configuration.incoming.artifacts.each { ResolvedArtifactResult artifact ->
                ComponentIdentifier identifier = artifact.id.componentIdentifier
                if (identifier instanceof ProjectComponentIdentifier || !DependencyUtils.isConsumable(artifact.file)) {
                    return
                }
                ExternalDependency dependency
                if (identifier instanceof ModuleComponentIdentifier) {
                    dependency = new ExternalDependency(
                            identifier.group,
                            identifier.module,
                            identifier.version,
                            artifact.file)
                } else {
                    dependency = ExternalDependency.fromLocal(artifact.file)
                }
                get(dependency, true, useFullDepname)
            }
        }

        if (cleanupDeps) {
            cleanup()
        }
    }

    private String getAarEntry(ExternalDependency dependency, Store store, String entry, String suffix) {
        if (!dependency.depFile.name.endsWith(".aar")) {
            return null
        }

        String key = dependency.cacheName
        String entryPath = store.get(key)
        if (entryPath == null || !Files.exists(Paths.get(entryPath))) {
            entryPath = ""
            File packagedEntry = getPackagedFile(dependency.depFile, new File(cacheDir, key), entry, suffix)
            if (packagedEntry != null) {
                entryPath = FileUtil.getRelativePath(rootProject.projectDir, packagedEntry)
            }
            store.set(key, entryPath)
        }
        if (entryPath) {
            copies.add(new File(rootProject.projectDir, entryPath))
        }

        return entryPath
    }

    private static File getPackagedFile(File aar, File destination, String entry, String suffix) {
        File packagedFile = new File(destination.parentFile, destination.name.replaceFirst(/\.aar$/, suffix))
        if (Files.exists(packagedFile.toPath())) {
            return packagedFile
        }

        FileSystem zipFile = FileSystems.newFileSystem(aar.toPath(), null)
        Path packagedPath = zipFile.getPath(entry)
        if (Files.exists(packagedPath)) {
            try {
                Files.copy(packagedPath, packagedFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } catch (IOException ignored) { }
            return packagedFile
        } else {
            return null
        }
    }
}
